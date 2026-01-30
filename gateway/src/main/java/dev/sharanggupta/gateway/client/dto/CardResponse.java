package dev.sharanggupta.gateway.client.dto;

public record CardResponse(
        String cardNumber,
        String cardType,
        int totalLimit,
        int amountUsed,
        int availableAmount
) {}