package dev.sharanggupta.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating a card.
 * Only requires card type and total limit - amount used defaults to 0.
 */
@Getter
public class CardCreateRequest {

    @NotBlank(message = "Card type is required")
    private final String cardType;

    @PositiveOrZero(message = "Total limit must be zero or positive")
    private final int totalLimit;

    @JsonCreator
    @Builder
    public CardCreateRequest(
            @JsonProperty("cardType") String cardType,
            @JsonProperty("totalLimit") int totalLimit) {
        this.cardType = cardType;
        this.totalLimit = totalLimit;
    }
}
