package io.github.kycguard.customer.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.enums.IdentifierType;
import io.github.kycguard.common.model.AddressDto;
import io.github.kycguard.common.model.IdentifierDto;
import io.github.kycguard.customer.dto.CreateCustomerRequest;
import io.github.kycguard.customer.exception.InvalidCustomerDataException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerValidatorTest {

    @Test
    @DisplayName("Should normalize names using NFKC and trim whitespaces")
    void shouldNormalizeNames() {
        String input = "  John   Doe  ";
        assertThat(CustomerValidator.normalizeName(input)).isEqualTo("John   Doe");

        // Unicode NFKC normalization
        String unicodeInput = " \uFB01nance  "; // ligature 'fi'
        assertThat(CustomerValidator.normalizeName(unicodeInput)).isEqualTo("finance");
    }

    @Test
    @DisplayName("Should reject invalid ISO country codes")
    void shouldRejectInvalidCountryCode() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "Jane Doe",
                List.of(),
                LocalDate.of(1990, 1, 1),
                null,
                "ZZ", // Invalid ISO code
                List.of(),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "P12345"))
        );

        assertThatThrownBy(() -> CustomerValidator.validate(request))
                .isInstanceOf(InvalidCustomerDataException.class)
                .hasMessageContaining("Invalid ISO-3166 alpha-2 country code");
    }

    @Test
    @DisplayName("Should reject individual without date of birth")
    void shouldRejectIndividualWithoutDateOfBirth() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "Jane Doe",
                List.of(),
                null,
                null,
                "US",
                List.of(),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "P12345"))
        );

        assertThatThrownBy(() -> CustomerValidator.validate(request))
                .isInstanceOf(InvalidCustomerDataException.class)
                .hasMessageContaining("Date of birth is required for individuals");
    }

    @Test
    @DisplayName("Should reject individual with future date of birth")
    void shouldRejectIndividualWithFutureDateOfBirth() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "Jane Doe",
                List.of(),
                LocalDate.now().plusDays(1),
                null,
                "US",
                List.of(),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "P12345"))
        );

        assertThatThrownBy(() -> CustomerValidator.validate(request))
                .isInstanceOf(InvalidCustomerDataException.class)
                .hasMessageContaining("Date of birth cannot be in the future");
    }

    @Test
    @DisplayName("Should reject individual with incorporation date")
    void shouldRejectIndividualWithIncorporationDate() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "Jane Doe",
                List.of(),
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2020, 1, 1),
                "US",
                List.of(),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "P12345"))
        );

        assertThatThrownBy(() -> CustomerValidator.validate(request))
                .isInstanceOf(InvalidCustomerDataException.class)
                .hasMessageContaining("Incorporation date must be null for individuals");
    }

    @Test
    @DisplayName("Should reject company without incorporation date")
    void shouldRejectCompanyWithoutIncorporationDate() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.COMPANY,
                "Acme Corp",
                List.of(),
                null,
                null,
                "US",
                List.of(),
                List.of(new IdentifierDto(IdentifierType.REGISTRATION_NO, "REG123"))
        );

        assertThatThrownBy(() -> CustomerValidator.validate(request))
                .isInstanceOf(InvalidCustomerDataException.class)
                .hasMessageContaining("Incorporation date is required for companies");
    }

    @Test
    @DisplayName("Should reject company with future incorporation date")
    void shouldRejectCompanyWithFutureIncorporationDate() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.COMPANY,
                "Acme Corp",
                List.of(),
                null,
                LocalDate.now().plusDays(5),
                "US",
                List.of(),
                List.of(new IdentifierDto(IdentifierType.REGISTRATION_NO, "REG123"))
        );

        assertThatThrownBy(() -> CustomerValidator.validate(request))
                .isInstanceOf(InvalidCustomerDataException.class)
                .hasMessageContaining("Incorporation date cannot be in the future");
    }

    @Test
    @DisplayName("Should pass validation for valid individual and company")
    void shouldPassValidRequests() {
        CreateCustomerRequest individual = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "Valid Individual",
                List.of(),
                LocalDate.of(1985, 5, 20),
                null,
                "GB",
                List.of(new AddressDto("10 Downing St", "London", "SW1A 2AA", "GB")),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "UK987654"))
        );
        CustomerValidator.validate(individual);

        CreateCustomerRequest company = new CreateCustomerRequest(
                CustomerType.COMPANY,
                "Valid Company Ltd",
                List.of(),
                null,
                LocalDate.of(2015, 3, 10),
                "DE",
                List.of(new AddressDto("Hauptstr 1", "Berlin", "10115", "DE")),
                List.of(new IdentifierDto(IdentifierType.REGISTRATION_NO, "HRB12345"))
        );
        CustomerValidator.validate(company);
    }
}
