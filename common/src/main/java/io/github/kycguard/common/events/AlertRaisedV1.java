package io.github.kycguard.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.enums.AlertSeverity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertRaisedV1(
        UUID alertId,
        UUID customerId,
        String ruleId,
        AlertSeverity severity,
        Instant windowStart,
        Instant windowEnd,
        List<UUID> txnIds,
        String explanation,
        Instant createdAt
) {
    public static final String TOPIC = "alerts.raised";
    public static final String EVENT_TYPE = "AlertRaised";
    public static final int SCHEMA_VERSION = 1;

    public AlertRaisedV1 {
        if (alertId == null) {
            alertId = UUID.randomUUID();
        }
        if (txnIds == null) {
            txnIds = List.of();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
