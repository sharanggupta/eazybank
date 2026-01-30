package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(name = "LoanInfo", description = "Customer loan information")
@Builder
public record LoanInfoDto(
        @Schema(description = "Unique loan number", example = "LN-001-234567")
        String loanNumber,

        @Schema(description = "Type of loan", example = "Home Loan")
        String loanType,

        @Schema(description = "Total loan amount", example = "500000")
        int totalLoan,

        @Schema(description = "Amount already paid towards the loan", example = "100000")
        int amountPaid,

        @Schema(description = "Outstanding loan amount", example = "400000")
        int outstandingAmount
) {}
