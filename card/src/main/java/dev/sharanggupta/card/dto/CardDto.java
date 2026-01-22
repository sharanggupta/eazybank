package dev.sharanggupta.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardDto {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    private String cardNumber;

    @NotBlank(message = "Card type is required")
    private String cardType;

    @PositiveOrZero(message = "Total limit must be zero or positive")
    private int totalLimit;

    @PositiveOrZero(message = "Amount used must be zero or positive")
    private int amountUsed;

    public int getAvailableAmount() {
        return totalLimit - amountUsed;
    }
}