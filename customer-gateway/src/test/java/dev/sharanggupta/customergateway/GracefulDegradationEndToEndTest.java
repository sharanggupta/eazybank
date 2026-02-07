package dev.sharanggupta.customergateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests for graceful degradation when optional services (card/loan) are unavailable.
 *
 * The gateway implements graceful degradation for non-critical services:
 * - Card service down: Response includes account data, card is omitted
 * - Loan service down: Response includes account data, loan is omitted
 * - Account service down: Returns error (no degradation - critical service)
 *
 * Additionally, when card/loan data doesn't exist, it's treated as "no data" (not an error).
 *
 * Uses high circuit breaker thresholds to prevent accidental trips during normal testing.
 */
@TestPropertySource(properties = {
        "test.context.id=GracefulDegradationEndToEndTest",
        "resilience4j.circuitbreaker.configs.default.sliding-window-size=100",
        "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=50",
        "resilience4j.circuitbreaker.configs.default.failure-rate-threshold=100",
        "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=60s"
})
class GracefulDegradationEndToEndTest extends BaseEndToEndTest {

    @Nested
    @DisplayName("When card service is unavailable")
    class CardServiceUnavailable {

        @Test
        @DisplayName("customer can still view their profile with account and loan data")
        void showsProfileWithoutCardData() {
            // Given
            stubAccountFetchSuccess(VALID_MOBILE);
            stubCardFetchUnavailable(VALID_MOBILE);
            stubLoanFetchSuccess(VALID_MOBILE);

            // When & Then
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo(VALID_NAME)
                    .jsonPath("$.account.accountNumber").isEqualTo(ACCOUNT_NUMBER)
                    .jsonPath("$.card").doesNotExist()
                    .jsonPath("$.loan.loanNumber").isEqualTo("123456789012");
        }
    }

    @Nested
    @DisplayName("When loan service is unavailable")
    class LoanServiceUnavailable {

        @Test
        @DisplayName("customer can still view their profile with account and card data")
        void showsProfileWithoutLoanData() {
            // Given
            stubAccountFetchSuccess(VALID_MOBILE);
            stubCardFetchSuccess(VALID_MOBILE);
            stubLoanFetchUnavailable(VALID_MOBILE);

            // When & Then
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo(VALID_NAME)
                    .jsonPath("$.account.accountNumber").isEqualTo(ACCOUNT_NUMBER)
                    .jsonPath("$.card.cardNumber").isEqualTo("1234567890123456")
                    .jsonPath("$.loan").doesNotExist();
        }
    }

    @Nested
    @DisplayName("When both card and loan services are unavailable")
    class MultipleServicesUnavailable {

        @Test
        @DisplayName("customer can still view their basic account profile")
        void showsBasicAccountProfile() {
            // Given
            stubAccountFetchSuccess(VALID_MOBILE);
            stubCardFetchUnavailable(VALID_MOBILE);
            stubLoanFetchUnavailable(VALID_MOBILE);

            // When & Then
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo(VALID_NAME)
                    .jsonPath("$.account.accountNumber").isEqualTo(ACCOUNT_NUMBER)
                    .jsonPath("$.card").doesNotExist()
                    .jsonPath("$.loan").doesNotExist();
        }
    }

    @Nested
    @DisplayName("When account service is unavailable (critical)")
    class AccountServiceUnavailable {

        @Test
        @DisplayName("customer cannot view their profile at all")
        void cannotViewProfile() {
            // Given: Account is critical - no graceful degradation
            stubAccountFetchUnavailable(VALID_MOBILE);
            stubCardFetchSuccess(VALID_MOBILE);
            stubLoanFetchSuccess(VALID_MOBILE);

            // When & Then
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("customer cannot view profile when all services are down")
        void cannotViewProfileWhenAllDown() {
            // Given
            stubAccountFetchUnavailable(VALID_MOBILE);
            stubCardFetchUnavailable(VALID_MOBILE);
            stubLoanFetchUnavailable(VALID_MOBILE);

            // When & Then
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    @Nested
    @DisplayName("When customer has no card or loan")
    class OptionalProductsNotFound {

        @Test
        @DisplayName("profile shows without card section when customer has no card")
        void showsProfileWithoutCardWhenNoneExists() {
            // Given: Customer has account and loan, but no card
            stubAccountFetchSuccess(VALID_MOBILE);
            stubCardFetchNotFound(VALID_MOBILE);
            stubLoanFetchSuccess(VALID_MOBILE);

            // When & Then
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.card").doesNotExist();
        }

        @Test
        @DisplayName("profile shows without loan section when customer has no loan")
        void showsProfileWithoutLoanWhenNoneExists() {
            // Given: Customer has account and card, but no loan
            stubAccountFetchSuccess(VALID_MOBILE);
            stubCardFetchSuccess(VALID_MOBILE);
            stubLoanFetchNotFound(VALID_MOBILE);

            // When & Then
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.loan").doesNotExist();
        }

        @Test
        @DisplayName("customer not found when account doesn't exist")
        void customerNotFoundWhenNoAccount() {
            // Given: Customer does not exist
            givenCustomerDoesNotExist(VALID_MOBILE);
            stubCardFetchSuccess(VALID_MOBILE);
            stubLoanFetchSuccess(VALID_MOBILE);

            // When & Then
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("Degradation behavior is consistent")
    class DegradationConsistency {

        @Test
        @DisplayName("repeated requests degrade the same way")
        void degradesConsistentlyAcrossRequests() {
            // Given
            stubAccountFetchSuccess(VALID_MOBILE);
            stubCardFetchUnavailable(VALID_MOBILE);
            stubLoanFetchSuccess(VALID_MOBILE);

            // When & Then: All requests should degrade the same way
            for (int i = 0; i < 3; i++) {
                client.get()
                        .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$.card").doesNotExist()
                        .jsonPath("$.loan.loanNumber").isEqualTo("123456789012");
            }
        }
    }
}
