package dev.sharanggupta.loan.service;

import dev.sharanggupta.loan.dto.LoanDto;

public interface LoanService {

    void createLoan(LoanDto loanDto);

    /**
     * Fetch loan details for a customer by mobile number.
     * Optimized query execution for improved performance.
     */
    LoanDto fetchLoan(String mobileNumber);

    void updateLoan(LoanDto loanDto);

    void deleteLoan(String mobileNumber);
}