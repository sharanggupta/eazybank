package dev.sharanggupta.loan.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for updating a loan.
 * Allows updating loan number, type, total loan amount, and amount paid.
 */
@Getter
public class LoanUpdateRequest {

    private final String loanNumber;

    private final String loanType;

    @PositiveOrZero(message = "Total loan must be zero or positive")
    private final int totalLoan;

    @PositiveOrZero(message = "Amount paid must be zero or positive")
    private final Integer amountPaid;

    @JsonCreator
    @Builder
    public LoanUpdateRequest(
            @JsonProperty("loanNumber") String loanNumber,
            @JsonProperty("loanType") String loanType,
            @JsonProperty("totalLoan") int totalLoan,
            @JsonProperty("amountPaid") Integer amountPaid) {
        this.loanNumber = loanNumber;
        this.loanType = loanType;
        this.totalLoan = totalLoan;
        this.amountPaid = amountPaid != null ? amountPaid : 0;
    }
}
