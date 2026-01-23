package dev.sharanggupta.card;

import dev.sharanggupta.card.dto.CardDto;
import dev.sharanggupta.card.dto.ResponseDto;
import dev.sharanggupta.card.repository.CardRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class CardEndToEndTest extends BaseEndToEndTest {

    private static final String CARD_API_PATH = "/card/api";
    private static final String VALID_MOBILE_NUMBER = "1234567890";
    private static final String CREDIT_CARD_TYPE = "Credit Card";
    private static final int DEFAULT_TOTAL_LIMIT = 100000;
    private static final String STATUS_201 = "201";
    private static final String CARD_CREATED_MESSAGE = "Card created successfully";

    @Autowired
    private CardRepository cardRepository;

    @AfterEach
    void tearDown() {
        cardRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create a new card")
    void shouldCreateCard() {
        CardDto cardRequest = createCardRequest(VALID_MOBILE_NUMBER, CREDIT_CARD_TYPE, DEFAULT_TOTAL_LIMIT);

        client.post()
                .uri(CARD_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cardRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ResponseDto.class)
                .value(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(STATUS_201);
                    assertThat(response.getStatusMessage()).isEqualTo(CARD_CREATED_MESSAGE);
                });
    }

    @Test
    @DisplayName("Should fetch card by mobile number")
    void shouldFetchCardByMobileNumber() {
        CardDto cardRequest = createCardRequest(VALID_MOBILE_NUMBER, CREDIT_CARD_TYPE, DEFAULT_TOTAL_LIMIT);

        createCard(cardRequest);

        client.get()
                .uri(CARD_API_PATH + "?mobileNumber=" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CardDto.class)
                .value(card -> {
                    assertThat(card.getMobileNumber()).isEqualTo(VALID_MOBILE_NUMBER);
                    assertThat(card.getCardType()).isEqualTo(CREDIT_CARD_TYPE);
                    assertThat(card.getTotalLimit()).isEqualTo(DEFAULT_TOTAL_LIMIT);
                    assertThat(card.getCardNumber()).isNotNull();
                    assertThat(card.getCardNumber()).hasSize(16);
                });
    }

    @Test
    @DisplayName("Should update card details")
    void shouldUpdateCard() {
        CardDto cardRequest = createCardRequest(VALID_MOBILE_NUMBER, CREDIT_CARD_TYPE, DEFAULT_TOTAL_LIMIT);
        createCard(cardRequest);

        CardDto existingCard = fetchCard(VALID_MOBILE_NUMBER);
        int updatedLimit = 200000;
        int amountUsed = 50000;
        existingCard.setTotalLimit(updatedLimit);
        existingCard.setAmountUsed(amountUsed);

        client.put()
                .uri(CARD_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(existingCard)
                .exchange()
                .expectStatus().isNoContent();

        CardDto updatedCard = fetchCard(VALID_MOBILE_NUMBER);
        assertThat(updatedCard.getTotalLimit()).isEqualTo(updatedLimit);
        assertThat(updatedCard.getAmountUsed()).isEqualTo(amountUsed);
        assertThat(updatedCard.getAvailableAmount()).isEqualTo(updatedLimit - amountUsed);
    }

    @Test
    @DisplayName("Should delete card by mobile number")
    void shouldDeleteCard() {
        CardDto cardRequest = createCardRequest(VALID_MOBILE_NUMBER, CREDIT_CARD_TYPE, DEFAULT_TOTAL_LIMIT);
        createCard(cardRequest);

        client.delete()
                .uri(CARD_API_PATH + "?mobileNumber=" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isNoContent();

        client.get()
                .uri(CARD_API_PATH + "?mobileNumber=" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should reject duplicate card creation")
    void shouldRejectDuplicateCardCreation() {
        CardDto cardRequest = createCardRequest(VALID_MOBILE_NUMBER, CREDIT_CARD_TYPE, DEFAULT_TOTAL_LIMIT);
        createCard(cardRequest);

        client.post()
                .uri(CARD_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cardRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should return not found for non-existent card")
    void shouldReturnNotFoundForNonExistentCard() {
        client.get()
                .uri(CARD_API_PATH + "?mobileNumber=9999999999")
                .exchange()
                .expectStatus().isNotFound();
    }

    private void createCard(CardDto cardDto) {
        client.post()
                .uri(CARD_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cardDto)
                .exchange()
                .expectStatus().isCreated();
    }

    private CardDto fetchCard(String mobileNumber) {
        return client.get()
                .uri(CARD_API_PATH + "?mobileNumber=" + mobileNumber)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CardDto.class)
                .returnResult()
                .getResponseBody();
    }

    private CardDto createCardRequest(String mobileNumber, String cardType, int totalLimit) {
        CardDto cardDto = new CardDto();
        cardDto.setMobileNumber(mobileNumber);
        cardDto.setCardType(cardType);
        cardDto.setTotalLimit(totalLimit);
        return cardDto;
    }
}