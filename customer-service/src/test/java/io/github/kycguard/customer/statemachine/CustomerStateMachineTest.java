package io.github.kycguard.customer.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.customer.exception.IllegalCustomerStateTransitionException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerStateMachineTest {

    private CustomerStateMachine stateMachine;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        stateMachine = new CustomerStateMachine();
        customerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should allow valid transitions from PENDING_SCREENING")
    void shouldAllowPendingTransitions() {
        assertThatCode(() -> stateMachine.validateTransition(customerId, CustomerStatus.PENDING_SCREENING, CustomerStatus.CLEARED))
                .doesNotThrowAnyException();
        assertThatCode(() -> stateMachine.validateTransition(customerId, CustomerStatus.PENDING_SCREENING, CustomerStatus.UNDER_REVIEW))
                .doesNotThrowAnyException();
        assertThatCode(() -> stateMachine.validateTransition(customerId, CustomerStatus.PENDING_SCREENING, CustomerStatus.REJECTED))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow valid transitions from UNDER_REVIEW")
    void shouldAllowUnderReviewTransitions() {
        assertThatCode(() -> stateMachine.validateTransition(customerId, CustomerStatus.UNDER_REVIEW, CustomerStatus.CLEARED))
                .doesNotThrowAnyException();
        assertThatCode(() -> stateMachine.validateTransition(customerId, CustomerStatus.UNDER_REVIEW, CustomerStatus.REJECTED))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow transition from CLEARED to UNDER_REVIEW on risk trigger")
    void shouldAllowClearedToUnderReview() {
        assertThatCode(() -> stateMachine.validateTransition(customerId, CustomerStatus.CLEARED, CustomerStatus.UNDER_REVIEW))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject illegal transitions")
    void shouldRejectIllegalTransitions() {
        // REJECTED is terminal
        assertThatThrownBy(() -> stateMachine.validateTransition(customerId, CustomerStatus.REJECTED, CustomerStatus.CLEARED))
                .isInstanceOf(IllegalCustomerStateTransitionException.class)
                .hasMessageContaining("Cannot transition customer");

        assertThatThrownBy(() -> stateMachine.validateTransition(customerId, CustomerStatus.REJECTED, CustomerStatus.PENDING_SCREENING))
                .isInstanceOf(IllegalCustomerStateTransitionException.class);

        // CLEARED cannot jump to PENDING_SCREENING or directly to REJECTED
        assertThatThrownBy(() -> stateMachine.validateTransition(customerId, CustomerStatus.CLEARED, CustomerStatus.PENDING_SCREENING))
                .isInstanceOf(IllegalCustomerStateTransitionException.class);

        assertThatThrownBy(() -> stateMachine.validateTransition(customerId, CustomerStatus.CLEARED, CustomerStatus.REJECTED))
                .isInstanceOf(IllegalCustomerStateTransitionException.class);
    }

    @Test
    @DisplayName("Should allow transition to identical status as no-op")
    void shouldAllowIdenticalStatusTransition() {
        assertThatCode(() -> stateMachine.validateTransition(customerId, CustomerStatus.CLEARED, CustomerStatus.CLEARED))
                .doesNotThrowAnyException();
        assertThat(stateMachine.canTransition(CustomerStatus.CLEARED, CustomerStatus.CLEARED)).isTrue();
    }
}
