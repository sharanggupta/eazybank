package dev.sharanggupta.loan.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LoanDto {

    private final String mobileNumber;

    private final String loanNumber;

    private final String loanType;

    @PositiveOrZero(message = "Total loan must be zero or positive")
    private final int totalLoan;

    @PositiveOrZero(message = "Amount paid must be zero or positive")
    private final int amountPaid;

    public int getOutstandingAmount() {
        return totalLoan - amountPaid;
    }

    @JsonCreator
    @Builder(toBuilder = true) // ✅ enable toBuilder for updates
    public LoanDto(
            @JsonProperty("mobileNumber") String mobileNumber,
            @JsonProperty("loanNumber") String loanNumber,
            @JsonProperty("loanType") String loanType,
            @JsonProperty("totalLoan") int totalLoan,
            @JsonProperty("amountPaid") int amountPaid) {
        this.mobileNumber = mobileNumber;
        this.loanNumber = loanNumber;
        this.loanType = loanType;
        this.totalLoan = totalLoan;
        this.amountPaid = amountPaid;
    }
}
