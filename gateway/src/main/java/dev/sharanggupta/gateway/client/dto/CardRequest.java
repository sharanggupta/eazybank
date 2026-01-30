package dev.sharanggupta.gateway.client.dto;

public record CardRequest(
        String mobileNumber,
        String cardNumber,
        String cardType,
        int totalLimit,
        int amountUsed
) {}