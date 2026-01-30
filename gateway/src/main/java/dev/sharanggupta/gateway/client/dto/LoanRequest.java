package dev.sharanggupta.gateway.client.dto;

public record LoanRequest(
        String mobileNumber,
        String loanNumber,
        String loanType,
        int totalLoan,
        int amountPaid
) {}