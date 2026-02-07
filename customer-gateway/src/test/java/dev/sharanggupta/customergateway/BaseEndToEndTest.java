package dev.sharanggupta.customergateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Base class for gateway end-to-end tests.
 *
 * Provides:
 * - WireMock servers for simulating downstream services
 * - Intention-revealing helper methods for test setup (Given phase)
 * - Common test data and expected values
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseEndToEndTest {

    // ==================== Test Data ====================

    protected static final String VALID_NAME = "John Doe";
    protected static final String VALID_EMAIL = "john@example.com";
    protected static final String VALID_MOBILE = "1234567890";
    protected static final String NON_EXISTENT_MOBILE = "9999999999";
    protected static final String ACCOUNT_NUMBER = "00010012345678901";

    // ==================== API Paths ====================

    protected static final String ONBOARD_PATH = "/api/customer/onboard";
    protected static final String DETAILS_PATH = "/api/customer/details";
    protected static final String UPDATE_PATH = "/api/customer/update";
    protected static final String OFFBOARD_PATH = "/api/customer/offboard";

    // ==================== Expected Response Messages ====================
    // These must match the controller's response messages

    protected static final String MESSAGE_ONBOARDED = "Customer onboarded successfully";
    protected static final String MESSAGE_UPDATED = "Customer details updated successfully";
    protected static final String MESSAGE_OFFBOARDED = "Customer offboarded successfully";

    protected static WireMockServer accountService;
    protected static WireMockServer cardService;
    protected static WireMockServer loanService;

    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;

    @LocalServerPort
    private int port;

    protected WebTestClient client;

    @BeforeAll
    static void startWireMockServers() {
        accountService = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        cardService = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        loanService = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

        accountService.start();
        cardService.start();
        loanService.start();
    }

    @AfterAll
    static void stopWireMockServers() {
        accountService.stop();
        cardService.stop();
        loanService.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Only configure WireMock URLs here - circuit breaker config is handled by @TestPropertySource
        // in each test class to ensure proper isolation
        registry.add("services.account-url", () -> "http://localhost:" + accountService.port());
        registry.add("services.card-url", () -> "http://localhost:" + cardService.port());
        registry.add("services.loan-url", () -> "http://localhost:" + loanService.port());
    }

    @BeforeEach
    void setUpClient() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();

        // Reset circuit breakers FIRST to clear any previous state
        resetAllCircuitBreakers();

        // Reset WireMock servers
        accountService.resetAll();
        cardService.resetAll();
        loanService.resetAll();
    }

    @AfterEach
    void tearDown() {
        // Reset circuit breakers after each test
        resetAllCircuitBreakers();
    }

    protected void resetAllCircuitBreakers() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            cb.reset();
            // Ensure circuit breaker is in CLOSED state
            cb.transitionToClosedState();
        });
    }

    // ========== Request/Response Builders ==========

    protected String createOnboardRequest(String name, String email, String mobile) {
        return """
            {"name": "%s", "email": "%s", "mobileNumber": "%s"}
            """.formatted(name, email, mobile);
    }

    protected String createUpdateRequest(String name, String email, String mobile, String accountNumber) {
        return """
            {
                "name": "%s",
                "email": "%s",
                "mobileNumber": "%s",
                "account": {
                    "accountNumber": "%s",
                    "accountType": "Savings",
                    "branchAddress": "456 New Address"
                }
            }
            """.formatted(name, email, mobile, accountNumber);
    }

    // ========== Account Service Stubs ==========

    protected void stubAccountCreateSuccess() {
        accountService.stubFor(post(urlEqualTo("/account/api"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"statusCode": "201", "statusMessage": "Account created successfully"}
                            """)));
    }

    protected void stubAccountCreateRejectsDuplicate() {
        accountService.stubFor(post(urlEqualTo("/account/api"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"errorMessage": "Customer already registered"}
                            """)));
    }

    protected void stubAccountFetchSuccess(String mobile) {
        accountService.stubFor(get(urlEqualTo("/account/api/" + mobile))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "name": "%s",
                                "email": "%s",
                                "mobileNumber": "%s",
                                "account": {
                                    "accountNumber": "%s",
                                    "accountType": "Savings",
                                    "branchAddress": "123 Main Street"
                                }
                            }
                            """.formatted(VALID_NAME, VALID_EMAIL, mobile, ACCOUNT_NUMBER))));
    }

    protected void stubAccountFetchNotFound(String mobile) {
        accountService.stubFor(get(urlEqualTo("/account/api/" + mobile))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"errorMessage": "Customer not found with mobileNumber : '%s'"}
                            """.formatted(mobile))));
    }

    protected void stubAccountFetchUnavailable(String mobile) {
        accountService.stubFor(get(urlEqualTo("/account/api/" + mobile))
                .willReturn(aResponse().withStatus(503)));
    }

    protected void stubAccountUpdateSuccess() {
        accountService.stubFor(put(urlEqualTo("/account/api"))
                .willReturn(aResponse().withStatus(204)));
    }

    protected void stubAccountUpdateNotFound() {
        accountService.stubFor(put(urlEqualTo("/account/api"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"errorMessage": "Account not found"}
                            """)));
    }

    protected void stubAccountDeleteSuccess(String mobile) {
        accountService.stubFor(delete(urlEqualTo("/account/api/" + mobile))
                .willReturn(aResponse().withStatus(204)));
    }

    protected void stubAccountDeleteNotFound(String mobile) {
        accountService.stubFor(delete(urlEqualTo("/account/api/" + mobile))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"errorMessage": "Customer not found"}
                            """)));
    }

    // ========== Card Service Stubs ==========

    protected void stubCardFetchSuccess(String mobile) {
        cardService.stubFor(get(urlEqualTo("/card/api/" + mobile))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "cardNumber": "1234567890123456",
                                "mobileNumber": "%s",
                                "cardType": "Credit Card",
                                "totalLimit": 100000,
                                "amountUsed": 5000,
                                "availableAmount": 95000
                            }
                            """.formatted(mobile))));
    }

    protected void stubCardFetchNotFound(String mobile) {
        cardService.stubFor(get(urlEqualTo("/card/api/" + mobile))
                .willReturn(aResponse().withStatus(404)));
    }

    protected void stubCardFetchUnavailable(String mobile) {
        cardService.stubFor(get(urlEqualTo("/card/api/" + mobile))
                .willReturn(aResponse().withStatus(503)));
    }

    protected void stubCardDeleteSuccess(String mobile) {
        cardService.stubFor(delete(urlEqualTo("/card/api/" + mobile))
                .willReturn(aResponse().withStatus(204)));
    }

    protected void stubCardDeleteNotFound(String mobile) {
        cardService.stubFor(delete(urlEqualTo("/card/api/" + mobile))
                .willReturn(aResponse().withStatus(404)));
    }

    // ========== Loan Service Stubs ==========

    protected void stubLoanFetchSuccess(String mobile) {
        loanService.stubFor(get(urlEqualTo("/loan/api/" + mobile))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "loanNumber": "123456789012",
                                "mobileNumber": "%s",
                                "loanType": "Home Loan",
                                "totalLoan": 500000,
                                "amountPaid": 50000,
                                "outstandingAmount": 450000
                            }
                            """.formatted(mobile))));
    }

    protected void stubLoanFetchNotFound(String mobile) {
        loanService.stubFor(get(urlEqualTo("/loan/api/" + mobile))
                .willReturn(aResponse().withStatus(404)));
    }

    protected void stubLoanFetchUnavailable(String mobile) {
        loanService.stubFor(get(urlEqualTo("/loan/api/" + mobile))
                .willReturn(aResponse().withStatus(503)));
    }

    protected void stubLoanDeleteSuccess(String mobile) {
        loanService.stubFor(delete(urlEqualTo("/loan/api/" + mobile))
                .willReturn(aResponse().withStatus(204)));
    }

    protected void stubLoanDeleteNotFound(String mobile) {
        loanService.stubFor(delete(urlEqualTo("/loan/api/" + mobile))
                .willReturn(aResponse().withStatus(404)));
    }

    // ========== Composite Stubs (Given phase helpers) ====================

    /**
     * Given: A customer exists with account, card, and loan data.
     */
    protected void givenCustomerExistsWithAllProducts(String mobile) {
        stubAccountFetchSuccess(mobile);
        stubCardFetchSuccess(mobile);
        stubLoanFetchSuccess(mobile);
    }

    /**
     * Given: A customer exists with account only (no card or loan).
     */
    protected void givenCustomerExistsWithAccountOnly(String mobile) {
        stubAccountFetchSuccess(mobile);
        stubCardFetchNotFound(mobile);
        stubLoanFetchNotFound(mobile);
    }

    /**
     * Given: All delete operations will succeed.
     */
    protected void givenAllDeletesWillSucceed(String mobile) {
        stubCardDeleteSuccess(mobile);
        stubLoanDeleteSuccess(mobile);
        stubAccountDeleteSuccess(mobile);
    }

    /**
     * Given: Customer does not exist.
     */
    protected void givenCustomerDoesNotExist(String mobile) {
        stubAccountFetchNotFound(mobile);
    }

    /**
     * Given: Account service will accept the onboard request.
     */
    protected void givenOnboardWillSucceed() {
        stubAccountCreateSuccess();
    }

    /**
     * Given: Account service will reject due to duplicate customer.
     */
    protected void givenOnboardWillFailDueToDuplicate() {
        stubAccountCreateRejectsDuplicate();
    }

    /**
     * Given: Account service will accept the update request.
     */
    protected void givenUpdateWillSucceed() {
        stubAccountUpdateSuccess();
    }

    /**
     * Given: Account service will reject update due to not found.
     */
    protected void givenUpdateWillFailDueToNotFound() {
        stubAccountUpdateNotFound();
    }
}