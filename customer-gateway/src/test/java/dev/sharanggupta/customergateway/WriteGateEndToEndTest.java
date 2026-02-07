package dev.sharanggupta.customergateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Write Gate protection when circuit breakers are open.
 *
 * The Write Gate is a safety mechanism that blocks ALL write operations
 * (onboard, update, offboard) when ANY circuit breaker is OPEN or HALF_OPEN.
 * This prevents partial data corruption across services.
 *
 * Key behaviors tested:
 * - When any service is degraded, all write operations are blocked
 * - Read operations continue to work with graceful degradation
 * - Writes resume after services recover
 *
 * Uses aggressive circuit breaker thresholds to quickly trigger open state.
 */
@TestPropertySource(properties = {
        "test.context.id=WriteGateEndToEndTest",
        "resilience4j.circuitbreaker.configs.default.sliding-window-size=2",
        "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=1",
        "resilience4j.circuitbreaker.configs.default.failure-rate-threshold=100",
        "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=30s",
        "resilience4j.circuitbreaker.configs.default.permitted-number-of-calls-in-half-open-state=1"
})
class WriteGateEndToEndTest extends BaseEndToEndTest {

    private static final int REQUESTS_TO_TRIP_CIRCUIT_BREAKER = 3;

    // ==================== Helper Methods ====================

    /**
     * Triggers a circuit breaker to OPEN state by making failing requests.
     * Uses the test's aggressive thresholds: sliding-window-size=2, minimum-calls=1, failure-rate=100%
     */
    private void triggerCircuitBreakerOpen(String serviceName) {
        givenServiceWillFail(serviceName);
        makeFailingRequestsToTripCircuitBreaker();
    }

    private void givenServiceWillFail(String serviceName) {
        switch (serviceName) {
            case "card_service" -> {
                stubAccountFetchSuccess(VALID_MOBILE);
                stubCardFetchUnavailable(VALID_MOBILE);
                stubLoanFetchSuccess(VALID_MOBILE);
            }
            case "loan_service" -> {
                stubAccountFetchSuccess(VALID_MOBILE);
                stubCardFetchSuccess(VALID_MOBILE);
                stubLoanFetchUnavailable(VALID_MOBILE);
            }
            case "account_service" -> {
                stubAccountFetchUnavailable(VALID_MOBILE);
                stubCardFetchSuccess(VALID_MOBILE);
                stubLoanFetchSuccess(VALID_MOBILE);
            }
        }
    }

    private void makeFailingRequestsToTripCircuitBreaker() {
        for (int i = 0; i < REQUESTS_TO_TRIP_CIRCUIT_BREAKER; i++) {
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange();
        }
    }

    // ==================== Test Classes ====================

    @Nested
    @DisplayName("When card service is degraded")
    class CardServiceDegraded {

        @Test
        @DisplayName("new customers cannot be onboarded")
        void cannotOnboardNewCustomers() {
            // Given
            triggerCircuitBreakerOpen("card_service");
            givenOnboardWillSucceed();

            // When & Then
            client.post()
                    .uri(ONBOARD_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createOnboardRequest("New User", "new@example.com", "5555555555"))
                    .exchange()
                    .expectStatus().is5xxServerError()
                    .expectBody()
                    .jsonPath("$.errorMessage").value(msg ->
                            assertThat((String) msg).containsIgnoringCase("circuit breaker"));
        }

        @Test
        @DisplayName("existing customers cannot be updated")
        void cannotUpdateExistingCustomers() {
            // Given
            triggerCircuitBreakerOpen("card_service");
            givenUpdateWillSucceed();

            // When & Then
            client.put()
                    .uri(UPDATE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createUpdateRequest("Updated", VALID_EMAIL, VALID_MOBILE, ACCOUNT_NUMBER))
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("customers cannot be offboarded")
        void cannotOffboardCustomers() {
            // Given
            triggerCircuitBreakerOpen("card_service");
            givenAllDeletesWillSucceed(VALID_MOBILE);

            // When & Then
            client.delete()
                    .uri(OFFBOARD_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    @Nested
    @DisplayName("When loan service is degraded")
    class LoanServiceDegraded {

        @Test
        @DisplayName("new customers cannot be onboarded")
        void cannotOnboardNewCustomers() {
            // Given
            triggerCircuitBreakerOpen("loan_service");
            givenOnboardWillSucceed();

            // When & Then
            client.post()
                    .uri(ONBOARD_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createOnboardRequest("New User", "new@example.com", "5555555555"))
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("existing customers cannot be updated")
        void cannotUpdateExistingCustomers() {
            // Given
            triggerCircuitBreakerOpen("loan_service");
            givenUpdateWillSucceed();

            // When & Then
            client.put()
                    .uri(UPDATE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createUpdateRequest("Updated", VALID_EMAIL, VALID_MOBILE, ACCOUNT_NUMBER))
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("customers cannot be offboarded")
        void cannotOffboardCustomers() {
            // Given
            triggerCircuitBreakerOpen("loan_service");
            givenAllDeletesWillSucceed(VALID_MOBILE);

            // When & Then
            client.delete()
                    .uri(OFFBOARD_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    @Nested
    @DisplayName("When account service is degraded")
    class AccountServiceDegraded {

        @Test
        @DisplayName("new customers cannot be onboarded")
        void cannotOnboardNewCustomers() {
            // Given
            triggerCircuitBreakerOpen("account_service");

            // Reconfigure stubs after triggering (circuit breaker remembers state)
            accountService.resetAll();
            givenOnboardWillSucceed();

            // When & Then: Write gate still blocks because CB is open
            client.post()
                    .uri(ONBOARD_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createOnboardRequest("New User", "new@example.com", "5555555555"))
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    @Nested
    @DisplayName("Read operations during degradation")
    class ReadsStillWork {

        @Test
        @DisplayName("customers can still view their profile when card service is degraded")
        void canViewProfileWhenCardDegraded() {
            // Given
            triggerCircuitBreakerOpen("card_service");

            // When & Then: Reads work, card data is gracefully degraded
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo(VALID_NAME)
                    .jsonPath("$.card").doesNotExist();
        }

        @Test
        @DisplayName("customers can still view their profile when loan service is degraded")
        void canViewProfileWhenLoanDegraded() {
            // Given
            triggerCircuitBreakerOpen("loan_service");

            // When & Then: Reads work, loan data is gracefully degraded
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo(VALID_NAME)
                    .jsonPath("$.loan").doesNotExist();
        }
    }

    @Nested
    @DisplayName("After services recover")
    class Recovery {

        @Test
        @DisplayName("write operations resume working normally")
        void writeOperationsResumeAfterRecovery() {
            // Given: Circuit breaker is open
            triggerCircuitBreakerOpen("card_service");

            // Verify writes are blocked
            givenOnboardWillSucceed();
            client.post()
                    .uri(ONBOARD_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createOnboardRequest("New User", "new@example.com", "5555555555"))
                    .exchange()
                    .expectStatus().is5xxServerError();

            // When: Circuit breaker is reset (simulating recovery)
            resetAllCircuitBreakers();
            accountService.resetAll();
            cardService.resetAll();
            loanService.resetAll();
            givenOnboardWillSucceed();

            // Then: Writes succeed
            client.post()
                    .uri(ONBOARD_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createOnboardRequest("New User", "new@example.com", "5555555555"))
                    .exchange()
                    .expectStatus().isCreated();
        }
    }

    @Nested
    @DisplayName("Error messages")
    class ErrorMessages {

        @Test
        @DisplayName("clearly indicate that the system is temporarily unavailable")
        void errorMessageIndicatesTemporaryUnavailability() {
            // Given
            triggerCircuitBreakerOpen("card_service");
            givenOnboardWillSucceed();

            // When & Then
            client.post()
                    .uri(ONBOARD_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createOnboardRequest("New User", "new@example.com", "5555555555"))
                    .exchange()
                    .expectStatus().is5xxServerError()
                    .expectBody()
                    .jsonPath("$.errorMessage").value(msg ->
                            assertThat((String) msg).containsIgnoringCase("circuit breaker"));
        }
    }
}
