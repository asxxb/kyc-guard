package io.github.kycguard.customer.service;

import io.github.kycguard.customer.domain.OutboxEventEntity;
import io.github.kycguard.customer.domain.OutboxStatus;
import io.github.kycguard.customer.repository.OutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;
    private final int maxRetries;

    public OutboxRelay(
            OutboxEventRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${customer.outbox.batch-size:50}") int batchSize,
            @Value("${customer.outbox.max-retries:3}") int maxRetries
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${customer.outbox.relay-interval-ms:500}")
    @Transactional
    public int relayPendingEvents() {
        List<OutboxEventEntity> pendingEvents = outboxRepository.findPendingEvents(
                OutboxStatus.PENDING,
                PageRequest.of(0, batchSize)
        );

        if (pendingEvents.isEmpty()) {
            return 0;
        }

        int publishedCount = 0;
        for (OutboxEventEntity event : pendingEvents) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        event.getTopic(),
                        event.getAggregateId(),
                        event.getPayload()
                );

                if (event.getCorrelationId() != null) {
                    record.headers().add(new RecordHeader("X-Correlation-Id", event.getCorrelationId().getBytes(StandardCharsets.UTF_8)));
                }

                kafkaTemplate.send(record).get();
                event.markPublished();
                publishedCount++;
                log.info("Relayed outbox event {} of type {} to topic {}", event.getId(), event.getEventType(), event.getTopic());
            } catch (Exception e) {
                log.error("Failed to relay outbox event {}", event.getId(), e);
                event.incrementRetry(maxRetries);
            }
        }
        outboxRepository.saveAll(pendingEvents);
        return publishedCount;
    }
}
