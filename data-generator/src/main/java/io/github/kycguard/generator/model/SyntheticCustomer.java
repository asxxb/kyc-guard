package io.github.kycguard.generator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.model.AddressDto;
import io.github.kycguard.common.model.IdentifierDto;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SyntheticCustomer(
        UUID customerId,
        CustomerType type,
        String fullName,
        List<String> aliases,
        LocalDate dateOfBirth,
        LocalDate incorporationDate,
        String country,
        List<AddressDto> addresses,
        List<IdentifierDto> identifiers,
        CustomerStatus status,
        boolean isRisky,
        String groundTruthWatchlistId,
        String groundTruthRiskCategory
) {
    public SyntheticCustomer {
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
