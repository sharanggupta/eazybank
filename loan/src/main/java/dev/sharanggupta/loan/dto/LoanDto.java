package dev.sharanggupta.loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanDto {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    private String loanNumber;

    @NotBlank(message = "Loan type is required")
    private String loanType;

    @PositiveOrZero(message = "Total loan must be zero or positive")
    private int totalLoan;

    @PositiveOrZero(message = "Amount paid must be zero or positive")
    private int amountPaid;

    public int getOutstandingAmount() {
        return totalLoan - amountPaid;
    }
}