package io.github.kycguard.customer.util;

import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.customer.dto.CreateCustomerRequest;
import io.github.kycguard.customer.exception.InvalidCustomerDataException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CustomerValidator {

    private static final Set<String> VALID_COUNTRY_CODES = new HashSet<>(Arrays.asList(Locale.getISOCountries()));

    private CustomerValidator() {
    }

    public static String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        return Normalizer.normalize(name.trim(), Normalizer.Form.NFKC);
    }

    public static void validate(CreateCustomerRequest request) {
        if (request.country() == null || !VALID_COUNTRY_CODES.contains(request.country().toUpperCase(Locale.ROOT))) {
            throw new InvalidCustomerDataException("country", "Invalid ISO-3166 alpha-2 country code: " + request.country());
        }

        if (request.type() == CustomerType.INDIVIDUAL) {
            if (request.dateOfBirth() == null) {
                throw new InvalidCustomerDataException("dateOfBirth", "Date of birth is required for individuals");
            }
            if (request.dateOfBirth().isAfter(LocalDate.now())) {
                throw new InvalidCustomerDataException("dateOfBirth", "Date of birth cannot be in the future");
            }
            if (request.incorporationDate() != null) {
                throw new InvalidCustomerDataException("incorporationDate", "Incorporation date must be null for individuals");
            }
        } else if (request.type() == CustomerType.COMPANY) {
            if (request.incorporationDate() == null) {
                throw new InvalidCustomerDataException("incorporationDate", "Incorporation date is required for companies");
            }
            if (request.incorporationDate().isAfter(LocalDate.now())) {
                throw new InvalidCustomerDataException("incorporationDate", "Incorporation date cannot be in the future");
            }
            if (request.dateOfBirth() != null) {
                throw new InvalidCustomerDataException("dateOfBirth", "Date of birth must be null for companies");
            }
        }
    }
}
