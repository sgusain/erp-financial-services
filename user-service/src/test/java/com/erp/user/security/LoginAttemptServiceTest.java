package com.erp.user.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the failed-attempt counting/locking logic. The lockout duration itself is a
 * hardcoded 15-minute constant (LOCKOUT_MINUTES) with no seam for injection, so a
 * real-time expiry test would need to either wait 15 real minutes or use a mocked
 * clock - neither of which the class currently supports (it calls LocalDateTime.now()
 * directly rather than through an injectable Clock). We therefore only test the
 * counting/locking behavior here (which is the security-critical part), not the
 * time-based unlock, and leave a note rather than adding an impractical sleep-based
 * test or refactoring production code beyond this test suite's scope.
 */
class LoginAttemptServiceTest {

    @Test
    void isLocked_falseInitially() {
        LoginAttemptService service = new LoginAttemptService();

        assertThat(service.isLocked("new-user@example.com")).isFalse();
    }

    @Test
    void isLocked_falseAfterFewerThanMaxFailures() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 4; i++) {
            service.recordFailure("user@example.com");
        }

        assertThat(service.isLocked("user@example.com")).isFalse();
    }

    @Test
    void isLocked_trueAfterFiveFailures() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 5; i++) {
            service.recordFailure("user@example.com");
        }

        assertThat(service.isLocked("user@example.com")).isTrue();
    }

    @Test
    void recordSuccess_resetsFailureCount() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 4; i++) {
            service.recordFailure("user@example.com");
        }
        service.recordSuccess("user@example.com");

        // Should take another full 5 failures to lock again, not just 1 more.
        service.recordFailure("user@example.com");
        assertThat(service.isLocked("user@example.com")).isFalse();
    }

    @Test
    void failuresAreTrackedIndependentlyPerUsername() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 5; i++) {
            service.recordFailure("locked-user@example.com");
        }

        assertThat(service.isLocked("locked-user@example.com")).isTrue();
        assertThat(service.isLocked("other-user@example.com")).isFalse();
    }
}
