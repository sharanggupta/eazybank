package dev.sharanggupta.customergateway.service;

import dev.sharanggupta.customergateway.client.CardServiceClient;
import dev.sharanggupta.customergateway.dto.CardInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private static final String CIRCUIT_BREAKER_NAME = "card_service";

    private final CardServiceClient cardServiceClient;

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFetchCard")
    public Mono<CardInfo> fetchCard(String mobileNumber) {
        log.debug("Fetching card for mobile: {}", mobileNumber);
        return cardServiceClient.fetchCard(mobileNumber);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackDeleteCard")
    public Mono<Void> deleteCard(String mobileNumber) {
        log.debug("Deleting card for mobile: {}", mobileNumber);
        return cardServiceClient.deleteCard(mobileNumber);
    }

    private Mono<CardInfo> fallbackFetchCard(String mobileNumber, Throwable throwable) {
        log.warn("Card service unavailable for mobile: {}", mobileNumber, throwable);
        return Mono.empty();
    }

    private Mono<Void> fallbackDeleteCard(String mobileNumber, Throwable throwable) {
        log.warn("Card service unavailable for delete, mobile: {}", mobileNumber, throwable);
        return Mono.empty();
    }
}
