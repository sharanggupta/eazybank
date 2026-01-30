package dev.sharanggupta.gateway.client.dto;

public record LoanResponse(
        String loanNumber,
        String loanType,
        int totalLoan,
        int amountPaid,
        int outstandingAmount
) {}