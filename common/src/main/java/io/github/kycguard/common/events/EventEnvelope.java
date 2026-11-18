package io.github.kycguard.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        String correlationId,
        T payload
) {
    public EventEnvelope {
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        if (schemaVersion <= 0) {
            schemaVersion = 1;
        }
    }

    public static <T> EventEnvelope<T> of(String eventType, int schemaVersion, String correlationId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                schemaVersion,
                Instant.now(),
                correlationId,
                payload
        );
    }

    public static <T> EventEnvelope<T> of(UUID eventId, String eventType, int schemaVersion, Instant occurredAt, String correlationId, T payload) {
        return new EventEnvelope<>(
                eventId,
                eventType,
                schemaVersion,
                occurredAt,
                correlationId,
                payload
        );
    }
}
