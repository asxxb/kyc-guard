package io.github.kycguard.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.model.AddressDto;
import io.github.kycguard.common.model.IdentifierDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerOnboardedV1(
        UUID customerId,
        CustomerType customerType,
        String fullName,
        List<String> aliases,
        LocalDate dateOfBirth,
        LocalDate incorporationDate,
        String country,
        List<AddressDto> addresses,
        List<IdentifierDto> identifiers,
        CustomerStatus status,
        Instant createdAt
) {
    public static final String TOPIC = "customer.onboarded";
    public static final String EVENT_TYPE = "CustomerOnboarded";
    public static final int SCHEMA_VERSION = 1;

    public CustomerOnboardedV1 {
        if (aliases == null) {
            aliases = List.of();
        }
        if (addresses == null) {
            addresses = List.of();
        }
        if (identifiers == null) {
            identifiers = List.of();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
