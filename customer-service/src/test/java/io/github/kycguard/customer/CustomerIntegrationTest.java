package io.github.kycguard.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.enums.IdentifierType;
import io.github.kycguard.common.events.CustomerOnboardedV1;
import io.github.kycguard.common.events.EventEnvelope;
import io.github.kycguard.common.model.AddressDto;
import io.github.kycguard.common.model.IdentifierDto;
import io.github.kycguard.common.util.JsonUtils;
import io.github.kycguard.customer.domain.CustomerEntity;
import io.github.kycguard.customer.domain.OutboxEventEntity;
import io.github.kycguard.customer.domain.OutboxStatus;
import io.github.kycguard.customer.dto.CreateCustomerRequest;
import io.github.kycguard.customer.dto.CustomerResponse;
import io.github.kycguard.customer.dto.UpdateStatusRequest;
import io.github.kycguard.customer.repository.CustomerRepository;
import io.github.kycguard.customer.repository.OutboxEventRepository;
import io.github.kycguard.customer.service.OutboxRelay;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CustomerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("kycguard_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("customer.outbox.relay-interval-ms", () -> 100000); // disable automatic poller in test to control manually
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setupClient() {
        restTemplate.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
    }

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private OutboxRelay outboxRelay;

    private static KafkaConsumer<String, String> kafkaConsumer;

    @BeforeAll
    static void setupConsumer() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-customer-group",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );
        kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(Collections.singletonList(CustomerOnboardedV1.TOPIC));
    }

    @AfterAll
    static void closeConsumer() {
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
    }

    @Test
    @DisplayName("End-to-End customer onboarding flow with PostgreSQL, Outbox, and Kafka")
    void shouldExecuteFullOnboardingFlow() {
        String baseUrl = "http://localhost:" + port + "/api/v1/customers";
        String idempotencyKey = "idemp-" + UUID.randomUUID();
        String correlationId = "corr-" + UUID.randomUUID();

        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                " Marcus Aurelius ",
                List.of("Philosopher King"),
                LocalDate.of(1980, 4, 26),
                null,
                "IT",
                List.of(new AddressDto("Via dei Fori Imperiali", "Rome", "00186", "IT")),
                List.of(new IdentifierDto(IdentifierType.NATIONAL_ID, "IT-ROM-12345"))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);
        headers.set("X-Correlation-Id", correlationId);

        // 1. Post Onboarding Request
        ResponseEntity<CustomerResponse> postResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                CustomerResponse.class
        );

        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CustomerResponse createdCustomer = postResponse.getBody();
        assertThat(createdCustomer).isNotNull();
        UUID customerId = createdCustomer.id();
        assertThat(createdCustomer.fullName()).isEqualTo("Marcus Aurelius"); // normalized & trimmed
        assertThat(createdCustomer.status()).isEqualTo(CustomerStatus.PENDING_SCREENING);

        // 2. Verify database entity exists
        Optional<CustomerEntity> dbCustomerOpt = customerRepository.findById(customerId);
        assertThat(dbCustomerOpt).isPresent();
        CustomerEntity dbCustomer = dbCustomerOpt.get();
        assertThat(dbCustomer.getCountry()).isEqualTo("IT");
        assertThat(dbCustomer.getAddresses()).hasSize(1);
        assertThat(dbCustomer.getIdentifiers()).hasSize(1);

        // 3. Verify Outbox record was created
        List<OutboxEventEntity> pendingEvents = outboxRepository.findPendingEvents(OutboxStatus.PENDING, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(pendingEvents).anyMatch(e -> e.getAggregateId().equals(customerId.toString()));

        // 4. Trigger Outbox Relay manually
        int relayed = outboxRelay.relayPendingEvents();
        assertThat(relayed).isGreaterThanOrEqualTo(1);

        // 5. Verify event was published to Kafka and consumed
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var records = kafkaConsumer.poll(Duration.ofMillis(500));
            assertThat(records.isEmpty()).isFalse();

            boolean found = false;
            for (ConsumerRecord<String, String> record : records) {
                if (record.key().equals(customerId.toString())) {
                    found = true;
                    Header corrHeader = record.headers().lastHeader("X-Correlation-Id");
                    assertThat(corrHeader).isNotNull();
                    assertThat(new String(corrHeader.value(), StandardCharsets.UTF_8)).isEqualTo(correlationId);

                    EventEnvelope<CustomerOnboardedV1> envelope = JsonUtils.fromJson(
                            record.value(),
                            new TypeReference<EventEnvelope<CustomerOnboardedV1>>() {}
                    );
                    assertThat(envelope.eventType()).isEqualTo(CustomerOnboardedV1.EVENT_TYPE);
                    assertThat(envelope.correlationId()).isEqualTo(correlationId);
                    assertThat(envelope.payload().customerId()).isEqualTo(customerId);
                    assertThat(envelope.payload().fullName()).isEqualTo("Marcus Aurelius");
                }
            }
            assertThat(found).isTrue();
        });

        // 6. Test Idempotency Replay (Same Key, Same Request Body)
        ResponseEntity<CustomerResponse> replayResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                CustomerResponse.class
        );
        assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replayResponse.getBody()).isNotNull();
        assertThat(replayResponse.getBody().id()).isEqualTo(customerId);

        // 7. Test Idempotency Conflict (Same Key, Different Request Body)
        CreateCustomerRequest conflictingRequest = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "Different Name",
                List.of(),
                LocalDate.of(1980, 4, 26),
                null,
                "IT",
                List.of(),
                List.of(new IdentifierDto(IdentifierType.NATIONAL_ID, "IT-ROM-12345"))
        );
        ResponseEntity<String> conflictResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                new HttpEntity<>(conflictingRequest, headers),
                String.class
        );
        assertThat(conflictResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflictResponse.getBody()).contains("Idempotency Conflict");

        // 8. Test GET /api/v1/customers/{id}
        ResponseEntity<CustomerResponse> getResponse = restTemplate.getForEntity(
                baseUrl + "/" + customerId,
                CustomerResponse.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().id()).isEqualTo(customerId);

        // 9. Test PATCH /api/v1/customers/{id}/status (Valid Transition: PENDING_SCREENING -> CLEARED)
        HttpHeaders patchHeaders = new HttpHeaders();
        patchHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<CustomerResponse> patchResponse = restTemplate.exchange(
                baseUrl + "/" + customerId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateStatusRequest(CustomerStatus.CLEARED), patchHeaders),
                CustomerResponse.class
        );
        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResponse.getBody()).isNotNull();
        assertThat(patchResponse.getBody().status()).isEqualTo(CustomerStatus.CLEARED);

        // 10. Test PATCH illegal transition (CLEARED -> REJECTED directly)
        ResponseEntity<String> illegalPatchResponse = restTemplate.exchange(
                baseUrl + "/" + customerId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateStatusRequest(CustomerStatus.REJECTED), patchHeaders),
                String.class
        );
        assertThat(illegalPatchResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(illegalPatchResponse.getBody()).contains("Illegal State Transition");
    }
}
