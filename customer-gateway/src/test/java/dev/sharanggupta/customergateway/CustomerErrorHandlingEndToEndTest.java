package dev.sharanggupta.customergateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests for error handling scenarios: customer not found and validation failures.
 *
 * Verifies that the gateway properly propagates error responses from downstream
 * services and validates input before forwarding requests.
 *
 * Uses high circuit breaker thresholds to prevent accidental trips during normal testing.
 */
@TestPropertySource(properties = {
        "test.context.id=CustomerErrorHandlingEndToEndTest",
        "resilience4j.circuitbreaker.configs.default.sliding-window-size=100",
        "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=50",
        "resilience4j.circuitbreaker.configs.default.failure-rate-threshold=100",
        "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=60s"
})
class CustomerErrorHandlingEndToEndTest extends BaseEndToEndTest {

    @Nested
    @DisplayName("When customer does not exist")
    class CustomerNotFound {

        @Test
        @DisplayName("cannot view details of a non-existent customer")
        void cannotViewNonExistentCustomer() {
            // Given
            givenCustomerDoesNotExist(NON_EXISTENT_MOBILE);

            // When & Then
            client.get()
                    .uri(DETAILS_PATH + "/" + NON_EXISTENT_MOBILE)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("cannot view details after customer has been deleted")
        void cannotViewDeletedCustomer() {
            // This test catches the bug where not found from account service
            // was incorrectly converted to server error by the circuit breaker fallback

            // Given: Customer initially exists
            givenCustomerExistsWithAllProducts(VALID_MOBILE);
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isOk();

            // When: Customer is deleted (simulate by changing stub to not found)
            accountService.resetAll();
            cardService.resetAll();
            loanService.resetAll();
            givenCustomerDoesNotExist(VALID_MOBILE);

            // Then: Should indicate customer not found
            client.get()
                    .uri(DETAILS_PATH + "/" + VALID_MOBILE)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("cannot update a non-existent customer")
        void cannotUpdateNonExistentCustomer() {
            // Given
            givenUpdateWillFailDueToNotFound();

            // When & Then
            client.put()
                    .uri(UPDATE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createUpdateRequest("John Updated", VALID_EMAIL, VALID_MOBILE, "9999999999999999"))
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("cannot offboard a non-existent customer")
        void cannotOffboardNonExistentCustomer() {
            // Given
            stubCardDeleteNotFound(NON_EXISTENT_MOBILE);
            stubLoanDeleteNotFound(NON_EXISTENT_MOBILE);
            stubAccountDeleteNotFound(NON_EXISTENT_MOBILE);

            // When & Then
            client.delete()
                    .uri(OFFBOARD_PATH + "/" + NON_EXISTENT_MOBILE)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("When customer already exists")
    class CustomerAlreadyExists {

        @Test
        @DisplayName("cannot onboard a customer who is already registered")
        void cannotOnboardExistingCustomer() {
            // Given
            givenOnboardWillFailDueToDuplicate();

            // When & Then
            client.post()
                    .uri(ONBOARD_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createOnboardRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE))
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    @DisplayName("Input validation")
    class ValidationErrors {

        @Nested
        @DisplayName("Mobile number must be valid")
        class MobileNumberValidation {

            @Test
            @DisplayName("rejects mobile number with fewer than 10 digits")
            void rejectsTooShortMobileNumber() {
                // When & Then (no Given - validation happens before downstream call)
                client.post()
                        .uri(ONBOARD_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(createOnboardRequest(VALID_NAME, VALID_EMAIL, "123"))
                        .exchange()
                        .expectStatus().isBadRequest();
            }

            @Test
            @DisplayName("rejects mobile number containing non-numeric characters")
            void rejectsNonNumericMobileNumber() {
                client.post()
                        .uri(ONBOARD_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(createOnboardRequest(VALID_NAME, VALID_EMAIL, "123abc4567"))
                        .exchange()
                        .expectStatus().isBadRequest();
            }

            @Test
            @DisplayName("rejects invalid mobile number when viewing details")
            void rejectsInvalidMobileWhenViewingDetails() {
                client.get()
                        .uri(DETAILS_PATH + "/123")
                        .exchange()
                        .expectStatus().isBadRequest();
            }
        }

        @Nested
        @DisplayName("Required fields must be provided")
        class RequiredFieldsValidation {

            @Test
            @DisplayName("rejects onboarding without a name")
            void rejectsOnboardingWithoutName() {
                client.post()
                        .uri(ONBOARD_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("""
                            {"email": "john@example.com", "mobileNumber": "1234567890"}
                            """)
                        .exchange()
                        .expectStatus().isBadRequest();
            }

            @Test
            @DisplayName("rejects onboarding without an email")
            void rejectsOnboardingWithoutEmail() {
                client.post()
                        .uri(ONBOARD_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("""
                            {"name": "John Doe", "mobileNumber": "1234567890"}
                            """)
                        .exchange()
                        .expectStatus().isBadRequest();
            }

            @Test
            @DisplayName("rejects onboarding without a mobile number")
            void rejectsOnboardingWithoutMobileNumber() {
                client.post()
                        .uri(ONBOARD_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("""
                            {"name": "John Doe", "email": "john@example.com"}
                            """)
                        .exchange()
                        .expectStatus().isBadRequest();
            }

            @Test
            @DisplayName("rejects onboarding with empty request")
            void rejectsEmptyOnboardingRequest() {
                client.post()
                        .uri(ONBOARD_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{}")
                        .exchange()
                        .expectStatus().isBadRequest();
            }
        }

        @Nested
        @DisplayName("Field formats must be valid")
        class FormatValidation {

            @Test
            @DisplayName("rejects invalid email format")
            void rejectsInvalidEmail() {
                client.post()
                        .uri(ONBOARD_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(createOnboardRequest(VALID_NAME, "not-an-email", VALID_MOBILE))
                        .exchange()
                        .expectStatus().isBadRequest();
            }

            @Test
            @DisplayName("rejects name that is too short")
            void rejectsTooShortName() {
                client.post()
                        .uri(ONBOARD_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(createOnboardRequest("Jo", VALID_EMAIL, VALID_MOBILE))
                        .exchange()
                        .expectStatus().isBadRequest();
            }
        }
    }
}
