package dev.sharanggupta.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class CustomerJourneyTest extends BaseEndToEndTest {

    // Test data constants
    private static final String CUSTOMER_MOBILE_NUMBER = "1234567890";
    private static final String CUSTOMER_NAME = "John Doe";
    private static final String CUSTOMER_EMAIL = "john@example.com";
    private static final String CARD_NUMBER = "1234-5678-9012-3456";
    private static final String CARD_TYPE = "Credit Card";
    private static final String LOAN_NUMBER = "LN-001-234567";
    private static final String LOAN_TYPE = "Home Loan";
    private static final String NONEXISTENT_MOBILE_NUMBER = "9999999999";

    @Nested
    @DisplayName("Onboard customer")
    class OnboardCustomer {

        @Test
        @DisplayName("should create account in downstream service")
        void shouldCreateAccount() {
            accountServiceStubs.stubCreateAccountSuccess();

            client.post().uri("/api/customer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"name": "%s", "email": "%s", "mobileNumber": "%s"}
                            """.formatted(CUSTOMER_NAME, CUSTOMER_EMAIL, CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isCreated();

            accountService.verify(postRequestedFor(urlEqualTo("/account/api/create")));
        }
    }

    @Nested
    @DisplayName("Get customer details")
    class GetCustomerDetails {

        @Test
        @DisplayName("should aggregate account, card, and loan when all services healthy")
        void shouldAggregateAllServices() {
            accountServiceStubs.stubFetchAccountSuccess(CUSTOMER_MOBILE_NUMBER, CUSTOMER_NAME, CUSTOMER_EMAIL);
            cardServiceStubs.stubFetchCardSuccess(CUSTOMER_MOBILE_NUMBER, CARD_NUMBER, CARD_TYPE);
            loanServiceStubs.stubFetchLoanSuccess(CUSTOMER_MOBILE_NUMBER, LOAN_NUMBER, LOAN_TYPE);

            client.get().uri("/api/customer/%s".formatted(CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.mobileNumber").isEqualTo(CUSTOMER_MOBILE_NUMBER)
                    .jsonPath("$.account.name").isEqualTo(CUSTOMER_NAME)
                    .jsonPath("$.card.cardNumber").isEqualTo(CARD_NUMBER)
                    .jsonPath("$.loan.loanNumber").isEqualTo(LOAN_NUMBER);
        }

        @Test
        @DisplayName("should return account and loan when card service is down (graceful degradation)")
        void shouldDegradeGracefullyWhenCardServiceDown() {
            accountServiceStubs.stubFetchAccountSuccess(CUSTOMER_MOBILE_NUMBER, CUSTOMER_NAME, CUSTOMER_EMAIL);
            cardServiceStubs.stubCardServiceDown();
            loanServiceStubs.stubFetchLoanSuccess(CUSTOMER_MOBILE_NUMBER, LOAN_NUMBER, LOAN_TYPE);

            client.get().uri("/api/customer/%s".formatted(CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.account.name").isEqualTo(CUSTOMER_NAME)
                    .jsonPath("$.card").doesNotExist()
                    .jsonPath("$.loan.loanNumber").isEqualTo(LOAN_NUMBER);
        }

        @Test
        @DisplayName("should return 404 when customer not found")
        void shouldReturn404WhenCustomerNotFound() {
            accountServiceStubs.stubFetchAccountNotFound(NONEXISTENT_MOBILE_NUMBER);

            client.get().uri("/api/customer/%s".formatted(NONEXISTENT_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("Card operations")
    class CardOperations {

        @Test
        @DisplayName("should return card details")
        void shouldReturnCardDetails() {
            cardServiceStubs.stubFetchCardSuccess(CUSTOMER_MOBILE_NUMBER, CARD_NUMBER, CARD_TYPE);

            client.get().uri("/api/customer/%s/card".formatted(CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.cardNumber").isEqualTo(CARD_NUMBER)
                    .jsonPath("$.cardType").isEqualTo(CARD_TYPE);
        }

        @Test
        @DisplayName("should return 404 when card not found")
        void shouldReturn404WhenCardNotFound() {
            cardServiceStubs.stubFetchCardNotFound(CUSTOMER_MOBILE_NUMBER);

            client.get().uri("/api/customer/%s/card".formatted(CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("should issue a new card")
        void shouldIssueCard() {
            cardServiceStubs.stubCreateCardSuccess();

            client.post().uri("/api/customer/%s/card".formatted(CUSTOMER_MOBILE_NUMBER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"cardType": "%s", "totalLimit": 100000}
                            """.formatted(CARD_TYPE))
                    .exchange()
                    .expectStatus().isCreated();

            cardService.verify(postRequestedFor(urlEqualTo("/card/api")));
        }
    }

    @Nested
    @DisplayName("Loan operations")
    class LoanOperations {

        @Test
        @DisplayName("should return loan details")
        void shouldReturnLoanDetails() {
            loanServiceStubs.stubFetchLoanSuccess(CUSTOMER_MOBILE_NUMBER, LOAN_NUMBER, LOAN_TYPE);

            client.get().uri("/api/customer/%s/loan".formatted(CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.loanNumber").isEqualTo(LOAN_NUMBER)
                    .jsonPath("$.loanType").isEqualTo(LOAN_TYPE);
        }

        @Test
        @DisplayName("should return 404 when loan not found")
        void shouldReturn404WhenLoanNotFound() {
            loanServiceStubs.stubFetchLoanNotFound(CUSTOMER_MOBILE_NUMBER);

            client.get().uri("/api/customer/%s/loan".formatted(CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("Offboard customer")
    class OffboardCustomer {

        @Test
        @DisplayName("should cancel card, close loan, and delete account")
        void shouldOffboardCustomer() {
            cardServiceStubs.stubDeleteCardSuccess();
            loanServiceStubs.stubDeleteLoanSuccess();
            accountServiceStubs.stubDeleteAccountSuccess();

            client.delete().uri("/api/customer/%s".formatted(CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isNoContent();

            cardService.verify(deleteRequestedFor(urlPathEqualTo("/card/api")));
            loanService.verify(deleteRequestedFor(urlPathEqualTo("/loan/api")));
            accountService.verify(deleteRequestedFor(urlPathEqualTo("/account/api/delete")));
        }

        @Test
        @DisplayName("should still delete account when card and loan services fail")
        void shouldDeleteAccountEvenWhenOtherServicesFail() {
            cardServiceStubs.stubCardServiceDown();
            loanServiceStubs.stubLoanServiceDown();
            accountServiceStubs.stubDeleteAccountSuccess();

            client.delete().uri("/api/customer/%s".formatted(CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isNoContent();

            accountService.verify(deleteRequestedFor(urlPathEqualTo("/account/api/delete")));
        }
    }
}
