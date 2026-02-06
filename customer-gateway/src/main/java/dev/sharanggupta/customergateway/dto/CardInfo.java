package dev.sharanggupta.customergateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardInfo(
        String cardNumber,
        String cardType,
        int totalLimit,
        int amountUsed,
        int availableAmount
) {}
