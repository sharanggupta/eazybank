package dev.sharanggupta.gateway.stubs;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Test stub helpers for account service integration tests.
 * Provides methods to configure WireMock responses for account-related API calls.
 */
public class AccountServiceTestStubs {

    private final WireMockServer wireMockServer;

    public AccountServiceTestStubs(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    /**
     * Stubs a successful account fetch response.
     *
     * @param mobileNumber the customer mobile number
     * @param accountName the account holder name
     * @param email the account holder email
     */
    public void stubFetchAccountSuccess(String mobileNumber, String accountName, String email) {
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/account/api/fetch"))
                .withQueryParam("mobileNumber", equalTo(mobileNumber))
                .willReturn(okJson("""
                        {"name": "%s", "email": "%s", "mobileNumber": "%s",
                         "accountDto": {"accountNumber": 1087654321, "accountType": "Savings", "branchAddress": "123 Main St"}}
                        """.formatted(accountName, email, mobileNumber))));
    }

    /**
     * Stubs a 404 response for account fetch when account not found.
     *
     * @param mobileNumber the customer mobile number
     */
    public void stubFetchAccountNotFound(String mobileNumber) {
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/account/api/fetch"))
                .withQueryParam("mobileNumber", equalTo(mobileNumber))
                .willReturn(notFound()));
    }

    /**
     * Stubs a successful account creation response.
     */
    public void stubCreateAccountSuccess() {
        wireMockServer.stubFor(WireMock.post(urlEqualTo("/account/api/create"))
                .willReturn(created()));
    }

    /**
     * Stubs a successful account deletion response.
     */
    public void stubDeleteAccountSuccess() {
        wireMockServer.stubFor(WireMock.delete(urlPathEqualTo("/account/api/delete"))
                .willReturn(noContent()));
    }
}