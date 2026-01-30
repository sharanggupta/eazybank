package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(name = "UpdateLoanRequest", description = "Request payload for updating loan details")
public record UpdateLoanRequest(
        @Schema(description = "Type of loan", example = "Home Loan")
        @NotBlank
        String loanType,

        @Schema(description = "Total loan amount", example = "500000")
        @PositiveOrZero
        int totalLoan,

        @Schema(description = "Amount already paid towards the loan", example = "100000")
        @PositiveOrZero
        int amountPaid
) {}
