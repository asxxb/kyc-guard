package io.github.kycguard.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.model.ScreeningMatchDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScreeningCompletedV1(
        UUID customerId,
        String customerName,
        List<ScreeningMatchDto> matches,
        Instant screenedAt
) {
    public static final String TOPIC = "screening.completed";
    public static final String EVENT_TYPE = "ScreeningCompleted";
    public static final int SCHEMA_VERSION = 1;

    public ScreeningCompletedV1 {
        if (matches == null) {
            matches = List.of();
        }
        if (screenedAt == null) {
            screenedAt = Instant.now();
        }
    }
}
