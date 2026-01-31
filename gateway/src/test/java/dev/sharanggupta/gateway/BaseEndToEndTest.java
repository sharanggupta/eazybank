package dev.sharanggupta.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.sharanggupta.gateway.stubs.AccountServiceTestStubs;
import dev.sharanggupta.gateway.stubs.CardServiceTestStubs;
import dev.sharanggupta.gateway.stubs.LoanServiceTestStubs;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseEndToEndTest {

    // Service configuration
    private static final String ACCOUNT_SERVICE_NAME = "account-service";
    private static final String CARD_SERVICE_NAME = "card-service";
    private static final String LOAN_SERVICE_NAME = "loan-service";
    private static final String[] SERVICES = {ACCOUNT_SERVICE_NAME, CARD_SERVICE_NAME, LOAN_SERVICE_NAME};

    // WireMock port configuration
    private static final int ACCOUNT_SERVICE_PORT = 8080;
    private static final int CARD_SERVICE_PORT = 9000;
    private static final int LOAN_SERVICE_PORT = 8090;

    // Circuit breaker configuration
    private static final int CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE = 2;
    private static final int CIRCUIT_BREAKER_MINIMUM_CALLS = 1;
    private static final int CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD = 50;
    private static final String CIRCUIT_BREAKER_WAIT_DURATION = "60s";

    @DynamicPropertySource
    static void overrideCircuitBreakerThresholds(DynamicPropertyRegistry registry) {
        for (String service : SERVICES) {
            String prefix = "resilience4j.circuitbreaker.instances." + service + ".";
            registry.add(prefix + "slidingWindowSize", () -> CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE);
            registry.add(prefix + "minimumNumberOfCalls", () -> CIRCUIT_BREAKER_MINIMUM_CALLS);
            registry.add(prefix + "failureRateThreshold", () -> CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD);
            registry.add(prefix + "waitDurationInOpenState", () -> CIRCUIT_BREAKER_WAIT_DURATION);
        }
    }

    protected static WireMockServer accountService;
    protected static WireMockServer cardService;
    protected static WireMockServer loanService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @LocalServerPort
    private int port;

    protected RestTestClient client;
    protected AccountServiceTestStubs accountServiceStubs;
    protected CardServiceTestStubs cardServiceStubs;
    protected LoanServiceTestStubs loanServiceStubs;

    @BeforeAll
    static void startWireMock() {
        accountService = new WireMockServer(wireMockConfig().port(ACCOUNT_SERVICE_PORT));
        cardService = new WireMockServer(wireMockConfig().port(CARD_SERVICE_PORT));
        loanService = new WireMockServer(wireMockConfig().port(LOAN_SERVICE_PORT));
        accountService.start();
        cardService.start();
        loanService.start();
    }

    @AfterAll
    static void stopWireMock() {
        accountService.stop();
        cardService.stop();
        loanService.stop();
    }

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        accountServiceStubs = new AccountServiceTestStubs(accountService);
        cardServiceStubs = new CardServiceTestStubs(cardService);
        loanServiceStubs = new LoanServiceTestStubs(loanService);
        accountService.resetAll();
        cardService.resetAll();
        loanService.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(CircuitBreaker::reset);
    }
}
