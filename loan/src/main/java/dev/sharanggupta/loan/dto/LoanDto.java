package dev.sharanggupta.loan.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LoanDto {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
    private final String mobileNumber;

    private final String loanNumber;

    @NotBlank(message = "Loan type is required")
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
