package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(name = "CreateLoanRequest", description = "Request payload for applying for a new loan")
public record CreateLoanRequest(
        @Schema(description = "Type of loan", example = "Home Loan")
        @NotBlank
        String loanType,

        @Schema(description = "Total loan amount", example = "500000")
        @PositiveOrZero
        int totalLoan
) {}
