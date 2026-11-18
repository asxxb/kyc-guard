package io.github.kycguard.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kycguard.customer.domain.OutboxEventEntity;
import io.github.kycguard.customer.domain.OutboxStatus;
import io.github.kycguard.customer.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(outboxRepository, kafkaTemplate, 50, 3);
    }

    @Test
    @DisplayName("Should successfully publish pending outbox events to Kafka and mark as published")
    void shouldRelayPendingEvents() {
        UUID eventId = UUID.randomUUID();
        OutboxEventEntity pendingEvent = new OutboxEventEntity(
                eventId,
                "Customer",
                "cust-123",
                "CustomerOnboarded",
                "customer.onboarded",
                "{\"foo\":\"bar\"}",
                "corr-1",
                OutboxStatus.PENDING,
                0,
                Instant.now(),
                null
        );

        when(outboxRepository.findPendingEvents(any(), any())).thenReturn(List.of(pendingEvent));

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("customer.onboarded", 0), 0, 0, 0, 0, 0);
        SendResult<String, String> sendResult = new SendResult<>(new ProducerRecord<>("customer.onboarded", "cust-123", "{}"), metadata);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(sendResult));

        int relayed = outboxRelay.relayPendingEvents();

        assertThat(relayed).isEqualTo(1);
        assertThat(pendingEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(pendingEvent.getPublishedAt()).isNotNull();

        ArgumentCaptor<List<OutboxEventEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(outboxRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).contains(pendingEvent);
    }

    @Test
    @DisplayName("Should increment retry count on Kafka send failure")
    void shouldHandleKafkaSendFailure() {
        UUID eventId = UUID.randomUUID();
        OutboxEventEntity pendingEvent = new OutboxEventEntity(
                eventId,
                "Customer",
                "cust-123",
                "CustomerOnboarded",
                "customer.onboarded",
                "{\"foo\":\"bar\"}",
                "corr-1",
                OutboxStatus.PENDING,
                0,
                Instant.now(),
                null
        );

        when(outboxRepository.findPendingEvents(any(), any())).thenReturn(List.of(pendingEvent));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unreachable")));

        int relayed = outboxRelay.relayPendingEvents();

        assertThat(relayed).isEqualTo(0);
        assertThat(pendingEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(pendingEvent.getRetryCount()).isEqualTo(1);
    }
}
