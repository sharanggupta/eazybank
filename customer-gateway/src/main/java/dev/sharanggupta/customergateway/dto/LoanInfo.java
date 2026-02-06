package dev.sharanggupta.customergateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoanInfo(
        String loanNumber,
        String loanType,
        int totalLoan,
        int amountPaid,
        int outstandingAmount
) {}
