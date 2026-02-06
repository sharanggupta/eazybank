package dev.sharanggupta.customergateway.client;

import dev.sharanggupta.customergateway.dto.CardInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CardServiceClient {

    private static final String CARD_URI = "card/api/{mobileNumber}";

    private final WebClient webClient;

    public CardServiceClient(@Qualifier("card") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<CardInfo> fetchCard(String mobileNumber) {
        return webClient.get()
                .uri(CARD_URI, mobileNumber)
                .retrieve()
                .bodyToMono(CardInfo.class)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.debug("No card found for mobile: {}", mobileNumber);
                    return Mono.empty();
                })
                .doOnError(e -> log.error("Error fetching card for mobile: {}", mobileNumber, e));
    }

    public Mono<Void> deleteCard(String mobileNumber) {
        return webClient.delete()
                .uri(CARD_URI, mobileNumber)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.debug("No card to delete for mobile: {}", mobileNumber);
                    return Mono.empty();
                })
                .doOnError(e -> log.error("Error deleting card for mobile: {}", mobileNumber, e));
    }
}
