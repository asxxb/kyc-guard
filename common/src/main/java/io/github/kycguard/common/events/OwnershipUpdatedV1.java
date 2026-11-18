package io.github.kycguard.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.model.BeneficialOwnerDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OwnershipUpdatedV1(
        UUID customerId,
        UUID entityId,
        String entityName,
        List<BeneficialOwnerDto> ultimateBeneficialOwners,
        int maxOwnershipDepth,
        Instant updatedAt
) {
    public static final String TOPIC = "ownership.updated";
    public static final String EVENT_TYPE = "OwnershipUpdated";
    public static final int SCHEMA_VERSION = 1;

    public OwnershipUpdatedV1 {
        if (ultimateBeneficialOwners == null) {
            ultimateBeneficialOwners = List.of();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
