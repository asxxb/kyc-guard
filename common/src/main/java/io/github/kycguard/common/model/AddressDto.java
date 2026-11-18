package io.github.kycguard.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AddressDto(
        String street,
        String city,
        String postalCode,
        String country
) {
}
