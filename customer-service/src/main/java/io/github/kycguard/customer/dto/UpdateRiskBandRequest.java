package io.github.kycguard.customer.dto;

import io.github.kycguard.common.enums.RiskBand;
import jakarta.validation.constraints.NotNull;

public record UpdateRiskBandRequest(
        @NotNull(message = "Risk band is required")
        RiskBand riskBand
) {
}
