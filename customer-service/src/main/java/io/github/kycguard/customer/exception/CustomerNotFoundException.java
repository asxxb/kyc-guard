package io.github.kycguard.customer.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

    private final UUID customerId;

    public CustomerNotFoundException(UUID customerId) {
        super("Customer not found with id: " + customerId);
        this.customerId = customerId;
    }

    public UUID getCustomerId() {
        return customerId;
    }
}
