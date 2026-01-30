package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(name = "CardInfo", description = "Customer card information")
@Builder
public record CardInfoDto(
        @Schema(description = "Unique card number", example = "1234-5678-9012-3456")
        String cardNumber,

        @Schema(description = "Type of card", example = "Credit Card")
        String cardType,

        @Schema(description = "Total credit limit on the card", example = "100000")
        int totalLimit,

        @Schema(description = "Amount already used on the card", example = "25000")
        int amountUsed,

        @Schema(description = "Available credit amount", example = "75000")
        int availableAmount
) {}
