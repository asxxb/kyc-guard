package io.github.kycguard.customer.exception;

public class InvalidCustomerDataException extends RuntimeException {

    private final String fieldName;

    public InvalidCustomerDataException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
