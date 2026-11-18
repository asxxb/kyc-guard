package io.github.kycguard.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.model.AddressDto;
import io.github.kycguard.common.model.IdentifierDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateCustomerRequest(
        @NotNull(message = "Customer type is required")
        CustomerType type,

        @NotBlank(message = "Full name is required")
        String fullName,

        List<String> aliases,

        LocalDate dateOfBirth,

        LocalDate incorporationDate,

        @NotBlank(message = "Country code is required")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be a valid ISO-3166 alpha-2 uppercase code")
        String country,

        @Valid
        List<AddressDto> addresses,

        @Valid
        @NotEmpty(message = "At least one identifier is required")
        List<IdentifierDto> identifiers
) {
    public CreateCustomerRequest {
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
