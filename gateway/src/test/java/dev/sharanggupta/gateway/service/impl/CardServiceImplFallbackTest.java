package dev.sharanggupta.gateway.service.impl;

import dev.sharanggupta.gateway.client.CardServiceClient;
import dev.sharanggupta.gateway.dto.CardInfoDto;
import dev.sharanggupta.gateway.dto.CreateCardRequest;
import dev.sharanggupta.gateway.exception.CardServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CardServiceImplFallbackTest {

    private CardServiceClient cardClient;
    private CardServiceImpl cardService;

    @BeforeEach
    void setUp() {
        cardClient = mock(CardServiceClient.class);
        cardService = new CardServiceImpl(cardClient);
    }

    @Test
    @DisplayName("getCard should return empty when card not found")
    void getCard_shouldReturnEmpty_whenNotFound() {
        String mobileNumber = "9876543210";
        when(cardClient.fetchCardByMobileNumber(mobileNumber))
                .thenReturn(Optional.empty());

        Optional<CardInfoDto> result = cardService.getCard(mobileNumber);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getCard should return card when found")
    void getCard_shouldReturnCard_whenFound() {
        String mobileNumber = "9876543210";
        CardInfoDto card = mock(CardInfoDto.class);
        when(cardClient.fetchCardByMobileNumber(mobileNumber))
                .thenReturn(Optional.of(card));

        Optional<CardInfoDto> result = cardService.getCard(mobileNumber);

        assertTrue(result.isPresent());
        assertEquals(card, result.get());
    }

    @Test
    @DisplayName("getCard should throw when service unavailable")
    void getCard_shouldThrow_whenServiceUnavailable() {
        String mobileNumber = "9876543210";
        when(cardClient.fetchCardByMobileNumber(mobileNumber))
                .thenThrow(new CardServiceException("Service unavailable"));

        assertThrows(CardServiceException.class, () -> cardService.getCard(mobileNumber));
    }

    @Test
    @DisplayName("issueCard should throw when service unavailable")
    void issueCard_shouldThrow_whenServiceUnavailable() {
        String mobileNumber = "9876543210";
        CreateCardRequest request = new CreateCardRequest("CREDIT", 50000);

        doThrow(new CardServiceException("Service unavailable"))
                .when(cardClient).createCard(mobileNumber, request.cardType(), request.totalLimit());

        assertThrows(CardServiceException.class, () -> cardService.issueCard(mobileNumber, request));
    }

    @Test
    @DisplayName("Read fallback method is present")
    void readFallbackMethod_shouldBePresent() {
        assertTrue(java.util.Arrays.stream(CardServiceImpl.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("getCardFallback")),
                "Fallback method getCardFallback should exist");
    }
}