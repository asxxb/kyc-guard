package io.github.kycguard.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.enums.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IdentifierDto(
        @NotNull IdentifierType type,
        @NotBlank String value
) {
}
