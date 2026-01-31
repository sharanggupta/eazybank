package dev.sharanggupta.gateway.service.impl;

import dev.sharanggupta.gateway.client.CardServiceClient;
import dev.sharanggupta.gateway.dto.CardInfoDto;
import dev.sharanggupta.gateway.dto.CreateCardRequest;
import dev.sharanggupta.gateway.dto.UpdateCardRequest;
import dev.sharanggupta.gateway.service.CardService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private static final String CIRCUIT_BREAKER = "card-service";

    private final CardServiceClient cardClient;

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "issueCardFallback")
    public void issueCard(String mobileNumber, CreateCardRequest request) {
        log.info("Issuing card for mobile number: {}", mobileNumber);
        cardClient.createCard(mobileNumber, request.cardType(), request.totalLimit());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "getCardFallback")
    public Optional<CardInfoDto> getCard(String mobileNumber) {
        log.info("Fetching card for mobile number: {}", mobileNumber);
        return cardClient.fetchCardByMobileNumber(mobileNumber);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "updateCardFallback")
    public void updateCard(String mobileNumber, UpdateCardRequest request) {
        log.info("Updating card for mobile number: {}", mobileNumber);
        cardClient.updateCard(mobileNumber, request.cardType(), request.totalLimit(), request.amountUsed());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "cancelCardFallback")
    public void cancelCard(String mobileNumber) {
        log.info("Cancelling card for mobile number: {}", mobileNumber);
        cardClient.deleteCard(mobileNumber);
    }

    private Optional<CardInfoDto> getCardFallback(String mobileNumber, Exception cause) {
        log.warn("Card service unavailable for {}", mobileNumber, cause);
        return Optional.empty();
    }

    private void issueCardFallback(String mobileNumber, CreateCardRequest request, Exception cause) {
        log.warn("Card service unavailable for issuing card to {}", mobileNumber, cause);
        // Circuit breaker will throw exception to WriteGateInterceptor
    }

    private void updateCardFallback(String mobileNumber, UpdateCardRequest request, Exception cause) {
        log.warn("Card service unavailable for updating card of {}", mobileNumber, cause);
        // Circuit breaker will throw exception to WriteGateInterceptor
    }

    private void cancelCardFallback(String mobileNumber, Exception cause) {
        log.warn("Card service unavailable for cancelling card of {}", mobileNumber, cause);
        // Circuit breaker will throw exception to WriteGateInterceptor
    }
}