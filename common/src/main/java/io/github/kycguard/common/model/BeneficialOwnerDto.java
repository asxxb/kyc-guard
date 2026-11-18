package io.github.kycguard.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.enums.RiskBand;
import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BeneficialOwnerDto(
        UUID ownerEntityId,
        String ownerName,
        String ownerType,
        BigDecimal ownershipPercentage,
        int depth,
        boolean isDirect,
        RiskBand riskBand
) {
}
