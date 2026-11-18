package io.github.kycguard.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.enums.RiskBand;
import io.github.kycguard.common.model.AddressDto;
import io.github.kycguard.common.model.IdentifierDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerResponse(
        UUID id,
        CustomerType type,
        String fullName,
        List<String> aliases,
        LocalDate dateOfBirth,
        LocalDate incorporationDate,
        String country,
        List<AddressDto> addresses,
        List<IdentifierDto> identifiers,
        CustomerStatus status,
        RiskBand riskBand,
        Instant createdAt,
        Instant updatedAt
) {
    public CustomerResponse {
        if (aliases == null) {
            aliases = List.of();
        }
        if (addresses == null) {
            addresses = List.of();
        }
        if (identifiers == null) {
            identifiers = List.of();
        }
    }
}
