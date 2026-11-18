package io.github.kycguard.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kycguard.common.util.JsonUtils;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    @Test
    @DisplayName("Should serialize and deserialize ErrorResponse without validation errors")
    void shouldSerializeAndDeserializeStandardErrorResponse() {
        ErrorResponse error = ErrorResponse.of(
                URI.create("https://kycguard.io/errors/customer-not-found"),
                "Customer Not Found",
                404,
                "Customer with id 12345 could not be found",
                URI.create("/api/v1/customers/12345")
        );

        String json = JsonUtils.toJson(error);
        assertThat(json)
                .contains("Customer Not Found")
                .contains("404")
                .contains("/api/v1/customers/12345");

        ErrorResponse deserialized = JsonUtils.fromJson(json, ErrorResponse.class);
        assertThat(deserialized.status()).isEqualTo(404);
        assertThat(deserialized.title()).isEqualTo("Customer Not Found");
        assertThat(deserialized.detail()).isEqualTo("Customer with id 12345 could not be found");
        assertThat(deserialized.invalidParams()).isNull();
    }

    @Test
    @DisplayName("Should serialize and deserialize ErrorResponse with validation errors")
    void shouldSerializeAndDeserializeValidationErrorResponse() {
        List<ErrorResponse.InvalidParam> invalidParams = List.of(
                new ErrorResponse.InvalidParam("country", "Invalid ISO-3166 alpha-2 country code", "XYZ"),
                new ErrorResponse.InvalidParam("dateOfBirth", "Date of birth cannot be in the future", "2099-01-01")
        );

        ErrorResponse error = ErrorResponse.ofValidation(
                URI.create("https://kycguard.io/errors/validation-failed"),
                "Validation Error",
                400,
                "Request body validation failed",
                URI.create("/api/v1/customers"),
                invalidParams
        );

        String json = JsonUtils.toJson(error);
        assertThat(json)
                .contains("Validation Error")
                .contains("country")
                .contains("dateOfBirth")
                .contains("XYZ");

        ErrorResponse deserialized = JsonUtils.fromJson(json, ErrorResponse.class);
        assertThat(deserialized.status()).isEqualTo(400);
        assertThat(deserialized.invalidParams()).hasSize(2);
        assertThat(deserialized.invalidParams().getFirst().name()).isEqualTo("country");
    }
}
