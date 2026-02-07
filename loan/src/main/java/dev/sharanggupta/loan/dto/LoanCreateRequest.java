package dev.sharanggupta.loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating a loan.
 * Only requires loan type and total loan - amount paid defaults to 0.
 */
@Getter
public class LoanCreateRequest {

    @NotBlank(message = "Loan type is required")
    private final String loanType;

    @PositiveOrZero(message = "Total loan must be zero or positive")
    private final int totalLoan;

    @JsonCreator
    @Builder
    public LoanCreateRequest(
            @JsonProperty("loanType") String loanType,
            @JsonProperty("totalLoan") int totalLoan) {
        this.loanType = loanType;
        this.totalLoan = totalLoan;
    }
}
