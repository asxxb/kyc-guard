package io.github.kycguard.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.enums.RiskBand;
import io.github.kycguard.common.model.RiskReasonDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RiskAssessedV1(
        UUID customerId,
        int score,
        RiskBand band,
        String modelVersionId,
        List<RiskReasonDto> reasons,
        Instant assessedAt
) {
    public static final String TOPIC = "risk.assessed";
    public static final String EVENT_TYPE = "RiskAssessed";
    public static final int SCHEMA_VERSION = 1;

    public RiskAssessedV1 {
        if (reasons == null) {
            reasons = List.of();
        }
        if (assessedAt == null) {
            assessedAt = Instant.now();
        }
    }
}
