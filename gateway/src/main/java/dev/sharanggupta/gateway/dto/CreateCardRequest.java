package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(name = "CreateCardRequest", description = "Request payload for issuing a new card")
public record CreateCardRequest(
        @Schema(description = "Type of card to issue", example = "Credit Card")
        @NotBlank
        String cardType,

        @Schema(description = "Total credit limit on the card", example = "100000")
        @PositiveOrZero
        int totalLimit
) {}
