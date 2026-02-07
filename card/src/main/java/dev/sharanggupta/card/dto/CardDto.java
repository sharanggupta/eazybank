package dev.sharanggupta.card.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CardDto {

    private final String mobileNumber;

    private final String cardNumber;

    private final String cardType;

    @PositiveOrZero(message = "Total limit must be zero or positive")
    private final int totalLimit;

    @PositiveOrZero(message = "Amount used must be zero or positive")
    private final int amountUsed;

    public int getAvailableAmount() {
        return totalLimit - amountUsed;
    }

    @JsonCreator
    @Builder
    public CardDto(
            @JsonProperty("mobileNumber") String mobileNumber,
            @JsonProperty("cardNumber") String cardNumber,
            @JsonProperty("cardType") String cardType,
            @JsonProperty("totalLimit") int totalLimit,
            @JsonProperty("amountUsed") int amountUsed) {
        this.mobileNumber = mobileNumber;
        this.cardNumber = cardNumber;
        this.cardType = cardType;
        this.totalLimit = totalLimit;
        this.amountUsed = amountUsed;
    }
}
