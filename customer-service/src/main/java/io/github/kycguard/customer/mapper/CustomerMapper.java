package io.github.kycguard.customer.mapper;

import io.github.kycguard.common.events.CustomerOnboardedV1;
import io.github.kycguard.common.model.AddressDto;
import io.github.kycguard.common.model.IdentifierDto;
import io.github.kycguard.customer.domain.CustomerEntity;
import io.github.kycguard.customer.dto.CustomerResponse;
import java.util.ArrayList;
import java.util.List;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerResponse toResponse(CustomerEntity entity) {
        List<AddressDto> addresses = entity.getAddresses().stream()
                .map(a -> new AddressDto(a.getStreet(), a.getCity(), a.getPostalCode(), a.getCountry()))
                .toList();

        List<IdentifierDto> identifiers = entity.getIdentifiers().stream()
                .map(i -> new IdentifierDto(i.getType(), i.getIdentifierValue()))
                .toList();

        return new CustomerResponse(
                entity.getId(),
                entity.getType(),
                entity.getFullName(),
                new ArrayList<>(entity.getAliases()),
                entity.getDateOfBirth(),
                entity.getIncorporationDate(),
                entity.getCountry(),
                addresses,
                identifiers,
                entity.getStatus(),
                entity.getRiskBand(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static CustomerOnboardedV1 toEvent(CustomerEntity entity) {
        List<AddressDto> addresses = entity.getAddresses().stream()
                .map(a -> new AddressDto(a.getStreet(), a.getCity(), a.getPostalCode(), a.getCountry()))
                .toList();

        List<IdentifierDto> identifiers = entity.getIdentifiers().stream()
                .map(i -> new IdentifierDto(i.getType(), i.getIdentifierValue()))
                .toList();

        return new CustomerOnboardedV1(
                entity.getId(),
                entity.getType(),
                entity.getFullName(),
                new ArrayList<>(entity.getAliases()),
                entity.getDateOfBirth(),
                entity.getIncorporationDate(),
                entity.getCountry(),
                addresses,
                identifiers,
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
