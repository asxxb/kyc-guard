package io.github.kycguard.customer.dto;

import io.github.kycguard.common.enums.CustomerStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "Status is required")
        CustomerStatus status
) {
}
