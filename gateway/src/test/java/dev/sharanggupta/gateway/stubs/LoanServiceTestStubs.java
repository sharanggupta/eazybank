package dev.sharanggupta.gateway.stubs;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Test stub helpers for loan service integration tests.
 * Provides methods to configure WireMock responses for loan-related API calls.
 */
public class LoanServiceTestStubs {

    private final WireMockServer wireMockServer;

    public LoanServiceTestStubs(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    /**
     * Stubs a successful loan fetch response.
     *
     * @param mobileNumber the customer mobile number
     * @param loanNumber the loan number
     * @param loanType the type of loan (e.g., "Home Loan")
     */
    public void stubFetchLoanSuccess(String mobileNumber, String loanNumber, String loanType) {
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/loan/api"))
                .withQueryParam("mobileNumber", equalTo(mobileNumber))
                .willReturn(okJson("""
                        {"loanNumber": "%s", "loanType": "%s",
                         "totalLoan": 500000, "amountPaid": 100000, "outstandingAmount": 400000}
                        """.formatted(loanNumber, loanType))));
    }

    /**
     * Stubs a 404 response for loan fetch when loan not found.
     *
     * @param mobileNumber the customer mobile number
     */
    public void stubFetchLoanNotFound(String mobileNumber) {
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/loan/api"))
                .withQueryParam("mobileNumber", equalTo(mobileNumber))
                .willReturn(notFound()));
    }

    /**
     * Stubs a successful loan deletion response.
     */
    public void stubDeleteLoanSuccess() {
        wireMockServer.stubFor(WireMock.delete(urlPathEqualTo("/loan/api"))
                .willReturn(noContent()));
    }

    /**
     * Stubs a service error response (500) for all loan service requests.
     * Used to simulate loan service being unavailable.
     */
    public void stubLoanServiceDown() {
        wireMockServer.stubFor(WireMock.any(anyUrl())
                .willReturn(serverError()));
    }
}