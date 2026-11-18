package io.github.kycguard.customer.exception;

import io.github.kycguard.common.enums.CustomerStatus;
import java.util.UUID;

public class IllegalCustomerStateTransitionException extends RuntimeException {

    private final UUID customerId;
    private final CustomerStatus currentStatus;
    private final CustomerStatus targetStatus;

    public IllegalCustomerStateTransitionException(UUID customerId, CustomerStatus currentStatus, CustomerStatus targetStatus) {
        super(String.format("Cannot transition customer %s from status %s to %s", customerId, currentStatus, targetStatus));
        this.customerId = customerId;
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public CustomerStatus getCurrentStatus() {
        return currentStatus;
    }

    public CustomerStatus getTargetStatus() {
        return targetStatus;
    }
}
