package dev.sharanggupta.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class WriteGateTest extends BaseEndToEndTest {

    // Test data constants
    private static final String CUSTOMER_MOBILE_NUMBER = "1234567890";
    private static final String NONEXISTENT_MOBILE_NUMBER = "1111111111";
    private static final String CUSTOMER_NAME = "John Doe";
    private static final String CUSTOMER_EMAIL = "john@example.com";
    private static final String LOAN_NUMBER = "LN-001-234567";
    private static final String LOAN_TYPE = "Home Loan";

    @Nested
    @DisplayName("When card service failures trip the circuit breaker")
    class CircuitBreakerTrippedByFailures {

        @BeforeEach
        void tripCardCircuitBreakerViaFailures() {
            cardServiceStubs.stubCardServiceDown();

            // A single failing card request trips the breaker (thresholds overridden to be minimal)
            client.get().uri("/api/customer/%s/card".formatted(NONEXISTENT_MOBILE_NUMBER))
                    .exchange();
        }

        @Test
        @DisplayName("should reject write requests with 503")
        void shouldRejectWrites() {
            client.post().uri("/api/customer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"name": "%s", "email": "%s", "mobileNumber": "%s"}
                            """.formatted(CUSTOMER_NAME, CUSTOMER_EMAIL, CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isEqualTo(503);
        }

        @Test
        @DisplayName("should still allow read requests with card omitted")
        void shouldAllowReadsWithCardOmitted() {
            accountServiceStubs.stubFetchAccountSuccess(CUSTOMER_MOBILE_NUMBER, CUSTOMER_NAME, CUSTOMER_EMAIL);
            loanServiceStubs.stubFetchLoanSuccess(CUSTOMER_MOBILE_NUMBER, LOAN_NUMBER, LOAN_TYPE);

            client.get().uri("/api/customer/%s".formatted(CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.account.name").isEqualTo(CUSTOMER_NAME)
                    .jsonPath("$.card").doesNotExist()
                    .jsonPath("$.loan.loanNumber").isEqualTo(LOAN_NUMBER);
        }
    }

    @Nested
    @DisplayName("When all circuit breakers are healthy")
    class CircuitBreakersHealthy {

        @Test
        @DisplayName("should allow write requests")
        void shouldAllowWrites() {
            accountServiceStubs.stubCreateAccountSuccess();

            client.post().uri("/api/customer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"name": "%s", "email": "%s", "mobileNumber": "%s"}
                            """.formatted(CUSTOMER_NAME, CUSTOMER_EMAIL, CUSTOMER_MOBILE_NUMBER))
                    .exchange()
                    .expectStatus().isCreated();
        }
    }
}
