package io.github.kycguard.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.enums.IdentifierType;
import io.github.kycguard.common.enums.RiskBand;
import io.github.kycguard.common.model.AddressDto;
import io.github.kycguard.common.model.IdentifierDto;
import io.github.kycguard.common.util.JsonUtils;
import io.github.kycguard.customer.domain.CustomerEntity;
import io.github.kycguard.customer.domain.IdempotencyRecordEntity;
import io.github.kycguard.customer.domain.OutboxEventEntity;
import io.github.kycguard.customer.dto.CreateCustomerRequest;
import io.github.kycguard.customer.dto.CustomerResponse;
import io.github.kycguard.customer.exception.CustomerNotFoundException;
import io.github.kycguard.customer.exception.IdempotencyConflictException;
import io.github.kycguard.customer.repository.CustomerRepository;
import io.github.kycguard.customer.repository.OutboxEventRepository;
import io.github.kycguard.customer.statemachine.CustomerStateMachine;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private CustomerStateMachine stateMachine;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, outboxRepository, idempotencyService, stateMachine);
    }

    @Test
    @DisplayName("Should successfully create customer, outbox record, and idempotency record")
    void shouldCreateCustomer() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                " Alice Smith ",
                List.of("Alice S"),
                LocalDate.of(1992, 4, 10),
                null,
                "US",
                List.of(new AddressDto("100 Elm St", "Boston", "02101", "US")),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "P98765432"))
        );

        when(idempotencyService.computeHash(any())).thenReturn("hash-123");
        when(idempotencyService.findExisting(eq("key-1"), eq("hash-123"))).thenReturn(Optional.empty());

        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.createCustomer(request, "key-1", "corr-test");

        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("Alice Smith"); // trimmed
        assertThat(response.status()).isEqualTo(CustomerStatus.PENDING_SCREENING);

        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        OutboxEventEntity savedOutbox = outboxCaptor.getValue();
        assertThat(savedOutbox.getEventType()).isEqualTo("CustomerOnboarded");
        assertThat(savedOutbox.getTopic()).isEqualTo("customer.onboarded");
        assertThat(savedOutbox.getCorrelationId()).isEqualTo("corr-test");

        verify(idempotencyService).record(eq("key-1"), eq("hash-123"), eq(201), any());
    }

    @Test
    @DisplayName("Should return cached response on identical idempotency replay")
    void shouldReturnCachedResponseOnIdempotencyReplay() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "Alice Smith",
                List.of(),
                LocalDate.of(1992, 4, 10),
                null,
                "US",
                List.of(),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "P98765432"))
        );

        UUID existingId = UUID.randomUUID();
        CustomerResponse cachedResponse = new CustomerResponse(
                existingId,
                CustomerType.INDIVIDUAL,
                "Alice Smith",
                List.of(),
                LocalDate.of(1992, 4, 10),
                null,
                "US",
                List.of(),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "P98765432")),
                CustomerStatus.PENDING_SCREENING,
                null,
                Instant.now(),
                Instant.now()
        );

        when(idempotencyService.computeHash(any())).thenReturn("hash-123");
        when(idempotencyService.findExisting("key-1", "hash-123")).thenReturn(
                Optional.of(new IdempotencyRecordEntity("key-1", "hash-123", 201, JsonUtils.toJson(cachedResponse), Instant.now()))
        );

        CustomerResponse response = customerService.createCustomer(request, "key-1", null);

        assertThat(response.id()).isEqualTo(existingId);
        verify(customerRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IdempotencyConflictException when key reused with different body")
    void shouldThrowOnIdempotencyConflict() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "Alice Smith",
                List.of(),
                LocalDate.of(1992, 4, 10),
                null,
                "US",
                List.of(),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "P98765432"))
        );

        when(idempotencyService.computeHash(any())).thenReturn("hash-different");
        when(idempotencyService.findExisting("key-1", "hash-different"))
                .thenThrow(new IdempotencyConflictException("key-1"));

        assertThatThrownBy(() -> customerService.createCustomer(request, "key-1", null))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    @DisplayName("Should retrieve customer by ID and throw 404 when not found")
    void shouldGetCustomerById() {
        UUID id = UUID.randomUUID();
        CustomerEntity entity = new CustomerEntity(
                id, CustomerType.INDIVIDUAL, "Bob Jones", null, LocalDate.of(1980, 1, 1),
                null, "GB", CustomerStatus.PENDING_SCREENING, null, Instant.now(), Instant.now()
        );

        when(customerRepository.findById(id)).thenReturn(Optional.of(entity));

        CustomerResponse response = customerService.getCustomer(id);
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.fullName()).isEqualTo("Bob Jones");

        UUID missingId = UUID.randomUUID();
        when(customerRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(missingId))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    @DisplayName("Should query customers by status and risk band with pagination")
    void shouldQueryCustomers() {
        CustomerEntity entity = new CustomerEntity(
                UUID.randomUUID(), CustomerType.INDIVIDUAL, "Bob Jones", null, LocalDate.of(1980, 1, 1),
                null, "GB", CustomerStatus.CLEARED, RiskBand.LOW, Instant.now(), Instant.now()
        );

        when(customerRepository.findByStatusAndRiskBand(eq(CustomerStatus.CLEARED), eq(RiskBand.LOW), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<CustomerResponse> page = customerService.getCustomers(CustomerStatus.CLEARED, RiskBand.LOW, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().fullName()).isEqualTo("Bob Jones");
    }

    @Test
    @DisplayName("Should validate state machine on status update")
    void shouldUpdateStatus() {
        UUID id = UUID.randomUUID();
        CustomerEntity entity = new CustomerEntity(
                id, CustomerType.INDIVIDUAL, "Bob Jones", null, LocalDate.of(1980, 1, 1),
                null, "GB", CustomerStatus.PENDING_SCREENING, null, Instant.now(), Instant.now()
        );

        when(customerRepository.findById(id)).thenReturn(Optional.of(entity));
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse updated = customerService.updateStatus(id, CustomerStatus.CLEARED);

        verify(stateMachine).validateTransition(id, CustomerStatus.PENDING_SCREENING, CustomerStatus.CLEARED);
        assertThat(updated.status()).isEqualTo(CustomerStatus.CLEARED);
    }

    @Test
    @DisplayName("Should update customer risk band")
    void shouldUpdateRiskBand() {
        UUID id = UUID.randomUUID();
        CustomerEntity entity = new CustomerEntity(
                id, CustomerType.INDIVIDUAL, "Bob Jones", null, LocalDate.of(1980, 1, 1),
                null, "GB", CustomerStatus.PENDING_SCREENING, null, Instant.now(), Instant.now()
        );

        when(customerRepository.findById(id)).thenReturn(Optional.of(entity));
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse updated = customerService.updateRiskBand(id, RiskBand.HIGH);
        assertThat(updated.riskBand()).isEqualTo(RiskBand.HIGH);
    }
}
