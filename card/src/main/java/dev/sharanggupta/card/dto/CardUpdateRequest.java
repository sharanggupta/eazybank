package dev.sharanggupta.card.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for updating a card.
 * Allows updating card number, type, limit, and amount used.
 */
@Getter
public class CardUpdateRequest {

    private final String cardNumber;

    private final String cardType;

    @PositiveOrZero(message = "Total limit must be zero or positive")
    private final int totalLimit;

    @PositiveOrZero(message = "Amount used must be zero or positive")
    private final Integer amountUsed;

    @JsonCreator
    @Builder
    public CardUpdateRequest(
            @JsonProperty("cardNumber") String cardNumber,
            @JsonProperty("cardType") String cardType,
            @JsonProperty("totalLimit") int totalLimit,
            @JsonProperty("amountUsed") Integer amountUsed) {
        this.cardNumber = cardNumber;
        this.cardType = cardType;
        this.totalLimit = totalLimit;
        this.amountUsed = amountUsed != null ? amountUsed : 0;
    }
}
