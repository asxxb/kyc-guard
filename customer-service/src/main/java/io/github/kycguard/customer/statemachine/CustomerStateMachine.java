package io.github.kycguard.customer.statemachine;

import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.customer.exception.IllegalCustomerStateTransitionException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustomerStateMachine {

    private static final Map<CustomerStatus, Set<CustomerStatus>> ALLOWED_TRANSITIONS = Map.of(
            CustomerStatus.PENDING_SCREENING, Set.of(
                    CustomerStatus.CLEARED,
                    CustomerStatus.UNDER_REVIEW,
                    CustomerStatus.REJECTED
            ),
            CustomerStatus.UNDER_REVIEW, Set.of(
                    CustomerStatus.CLEARED,
                    CustomerStatus.REJECTED
            ),
            CustomerStatus.CLEARED, Set.of(
                    CustomerStatus.UNDER_REVIEW
            ),
            CustomerStatus.REJECTED, Set.of() // Terminal state
    );

    public void validateTransition(UUID customerId, CustomerStatus currentStatus, CustomerStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        Set<CustomerStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new IllegalCustomerStateTransitionException(customerId, currentStatus, targetStatus);
        }
    }

    public boolean canTransition(CustomerStatus currentStatus, CustomerStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return true;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }
}
