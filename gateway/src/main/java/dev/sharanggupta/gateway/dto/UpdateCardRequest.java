package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(name = "UpdateCardRequest", description = "Request payload for updating card details")
public record UpdateCardRequest(
        @Schema(description = "Type of card", example = "Credit Card")
        @NotBlank
        String cardType,

        @Schema(description = "Total credit limit on the card", example = "150000")
        @PositiveOrZero
        int totalLimit,

        @Schema(description = "Amount already used on the card", example = "25000")
        @PositiveOrZero
        int amountUsed
) {}
