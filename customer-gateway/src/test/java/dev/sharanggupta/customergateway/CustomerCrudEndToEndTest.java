package dev.sharanggupta.customergateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests for basic CRUD operations: onboard, details, update, offboard.
 *
 * Uses high circuit breaker thresholds to prevent accidental trips during normal testing.
 */
@TestPropertySource(properties = {
        "test.context.id=CustomerCrudEndToEndTest",
        "resilience4j.circuitbreaker.configs.default.sliding-window-size=100",
        "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=50",
        "resilience4j.circuitbreaker.configs.default.failure-rate-threshold=100",
        "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=60s"
})
class CustomerCrudEndToEndTest extends BaseEndToEndTest {

    @Nested
    @DisplayName("Onboarding a new customer")
    class OnboardCustomer {

        @Test
        @DisplayName("successfully onboards a new customer")
        void successfullyOnboardsNewCustomer() {
            // Given
            givenOnboardWillSucceed();

            // When & Then
            client.post()
                    .uri(ONBOARD_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createOnboardRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.statusMessage").isEqualTo(MESSAGE_ONBOARDED);
        }
    }

    @Nested
    @DisplayName("Viewing customer details")
    class GetCustomerDetails {

        @Test
        @DisplayName("shows complete profile including account, card, and loan")
        void showsCompleteProfile() {
            // Given
            givenCustomerExistsWithAllProducts(VALID_MOBILE);

            // When & Then
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo(VALID_NAME)
                    .jsonPath("$.email").isEqualTo(VALID_EMAIL)
                    .jsonPath("$.mobileNumber").isEqualTo(VALID_MOBILE)
                    .jsonPath("$.account.accountNumber").isEqualTo(ACCOUNT_NUMBER)
                    .jsonPath("$.account.accountType").isEqualTo("Savings")
                    .jsonPath("$.card.cardNumber").isEqualTo("1234567890123456")
                    .jsonPath("$.card.cardType").isEqualTo("Credit Card")
                    .jsonPath("$.loan.loanNumber").isEqualTo("123456789012")
                    .jsonPath("$.loan.loanType").isEqualTo("Home Loan");
        }

        @Test
        @DisplayName("shows profile without card when customer has no card")
        void showsProfileWithoutCard() {
            // Given
            stubAccountFetchSuccess(VALID_MOBILE);
            stubCardFetchNotFound(VALID_MOBILE);
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

        @Test
        @DisplayName("shows profile without loan when customer has no loan")
        void showsProfileWithoutLoan() {
            // Given
            stubAccountFetchSuccess(VALID_MOBILE);
            stubCardFetchSuccess(VALID_MOBILE);
            stubLoanFetchNotFound(VALID_MOBILE);

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
    @DisplayName("Updating customer details")
    class UpdateCustomer {

        @Test
        @DisplayName("successfully updates customer information")
        void successfullyUpdatesCustomer() {
            // Given
            givenUpdateWillSucceed();

            // When & Then
            client.put()
                    .uri(UPDATE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createUpdateRequest("John Updated", VALID_EMAIL, VALID_MOBILE, ACCOUNT_NUMBER))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.statusMessage").isEqualTo(MESSAGE_UPDATED);
        }
    }

    @Nested
    @DisplayName("Offboarding a customer")
    class OffboardCustomer {

        @Test
        @DisplayName("successfully offboards customer and removes all products")
        void successfullyOffboardsCustomer() {
            // Given
            givenAllDeletesWillSucceed(VALID_MOBILE);

            // When & Then
            client.delete()
                    .uri(OFFBOARD_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.statusMessage").isEqualTo(MESSAGE_OFFBOARDED);
        }

        @Test
        @DisplayName("successfully offboards customer who has no card or loan")
        void successfullyOffboardsCustomerWithNoProducts() {
            // Given: Customer has no card or loan (not found responses are ignored)
            stubCardDeleteNotFound(VALID_MOBILE);
            stubLoanDeleteNotFound(VALID_MOBILE);
            stubAccountDeleteSuccess(VALID_MOBILE);

            // When & Then
            client.delete()
                    .uri(OFFBOARD_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.statusMessage").isEqualTo(MESSAGE_OFFBOARDED);
        }
    }

    @Nested
    @DisplayName("Complete customer lifecycle")
    class FullLifecycle {

        @Test
        @DisplayName("customer can be onboarded, viewed, updated, offboarded, and then no longer exists")
        void completesFullCustomerJourney() {
            // Step 1: Onboard
            givenOnboardWillSucceed();
            client.post()
                    .uri(ONBOARD_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createOnboardRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE))
                    .exchange()
                    .expectStatus().isCreated();

            // Step 2: Fetch
            accountService.resetAll();
            givenCustomerExistsWithAllProducts(VALID_MOBILE);
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo(VALID_NAME);

            // Step 3: Update
            accountService.resetAll();
            givenUpdateWillSucceed();
            client.put()
                    .uri(UPDATE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createUpdateRequest("John Updated", VALID_EMAIL, VALID_MOBILE, ACCOUNT_NUMBER))
                    .exchange()
                    .expectStatus().isOk();

            // Step 4: Offboard
            accountService.resetAll();
            cardService.resetAll();
            loanService.resetAll();
            givenAllDeletesWillSucceed(VALID_MOBILE);
            client.delete()
                    .uri(OFFBOARD_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk();

            // Step 5: Verify customer no longer exists
            accountService.resetAll();
            givenCustomerDoesNotExist(VALID_MOBILE);
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }
}
