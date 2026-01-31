package dev.sharanggupta.gateway.stubs;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Test stub helpers for card service integration tests.
 * Provides methods to configure WireMock responses for card-related API calls.
 */
public class CardServiceTestStubs {

    private final WireMockServer wireMockServer;

    public CardServiceTestStubs(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    /**
     * Stubs a successful card fetch response.
     *
     * @param mobileNumber the customer mobile number
     * @param cardNumber the card number
     * @param cardType the type of card (e.g., "Credit Card")
     */
    public void stubFetchCardSuccess(String mobileNumber, String cardNumber, String cardType) {
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/card/api"))
                .withQueryParam("mobileNumber", equalTo(mobileNumber))
                .willReturn(okJson("""
                        {"cardNumber": "%s", "cardType": "%s",
                         "totalLimit": 100000, "amountUsed": 25000, "availableAmount": 75000}
                        """.formatted(cardNumber, cardType))));
    }

    /**
     * Stubs a 404 response for card fetch when card not found.
     *
     * @param mobileNumber the customer mobile number
     */
    public void stubFetchCardNotFound(String mobileNumber) {
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/card/api"))
                .withQueryParam("mobileNumber", equalTo(mobileNumber))
                .willReturn(notFound()));
    }

    /**
     * Stubs a successful card creation response.
     */
    public void stubCreateCardSuccess() {
        wireMockServer.stubFor(WireMock.post(urlEqualTo("/card/api"))
                .willReturn(created()));
    }

    /**
     * Stubs a successful card deletion response.
     */
    public void stubDeleteCardSuccess() {
        wireMockServer.stubFor(WireMock.delete(urlPathEqualTo("/card/api"))
                .willReturn(noContent()));
    }

    /**
     * Stubs a service error response (500) for all card service requests.
     * Used to simulate card service being unavailable.
     */
    public void stubCardServiceDown() {
        wireMockServer.stubFor(WireMock.any(anyUrl())
                .willReturn(serverError()));
    }
}