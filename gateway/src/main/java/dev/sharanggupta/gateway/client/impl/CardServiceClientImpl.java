package dev.sharanggupta.gateway.client.impl;

import dev.sharanggupta.gateway.client.CardServiceClient;
import dev.sharanggupta.gateway.client.dto.CardRequest;
import dev.sharanggupta.gateway.client.dto.CardResponse;
import dev.sharanggupta.gateway.dto.CardInfoDto;
import dev.sharanggupta.gateway.exception.CardServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
@Slf4j
public class CardServiceClientImpl implements CardServiceClient {

    private final RestClient restClient;
    private final String cardServiceUrl;

    public CardServiceClientImpl(RestClient restClient,
                                 @Value("${services.card-url}") String cardServiceUrl) {
        this.restClient = restClient;
        this.cardServiceUrl = cardServiceUrl;
    }

    @Override
    public Optional<CardInfoDto> fetchCardByMobileNumber(String mobileNumber) {
        log.debug("Fetching card for mobile number: {}", mobileNumber);

        try {
            return restClient.get()
                    .uri(cardServiceUrl + "/card/api?mobileNumber={mobileNumber}", mobileNumber)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is4xxClientError()) {
                            log.debug("No card found for mobile number: {}", mobileNumber);
                            return Optional.<CardInfoDto>empty();
                        }
                        if (response.getStatusCode().isError()) {
                            throw new CardServiceException("returned " + response.getStatusCode());
                        }
                        CardResponse body = response.bodyTo(CardResponse.class);
                        return Optional.of(new CardInfoDto(
                                body.cardNumber(), body.cardType(),
                                body.totalLimit(), body.amountUsed(), body.availableAmount()));
                    });
        } catch (CardServiceException e) {
            throw e;
        } catch (RestClientException e) {
            throw new CardServiceException(e.getMessage());
        }
    }

    @Override
    public void createCard(String mobileNumber, String cardType, int totalLimit) {
        log.debug("Creating card for mobile number: {}", mobileNumber);

        try {
            restClient.post()
                    .uri(cardServiceUrl + "/card/api")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CardRequest(mobileNumber, null, cardType, totalLimit, 0))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RestClientException e) {
            throw new CardServiceException(e.getMessage());
        }
    }

    @Override
    public void updateCard(String mobileNumber, String cardType, int totalLimit, int amountUsed) {
        log.debug("Updating card for mobile number: {}", mobileNumber);

        try {
            restClient.put()
                    .uri(cardServiceUrl + "/card/api")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CardRequest(mobileNumber, null, cardType, totalLimit, amountUsed))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RestClientException e) {
            throw new CardServiceException(e.getMessage());
        }
    }

    @Override
    public void deleteCard(String mobileNumber) {
        log.debug("Deleting card for mobile number: {}", mobileNumber);

        try {
            restClient.delete()
                    .uri(cardServiceUrl + "/card/api?mobileNumber={mobileNumber}", mobileNumber)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) ->
                            log.debug("Card not found for mobile number: {}", mobileNumber))
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new CardServiceException("returned " + response.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (CardServiceException e) {
            throw e;
        } catch (RestClientException e) {
            throw new CardServiceException(e.getMessage());
        }
    }
}
