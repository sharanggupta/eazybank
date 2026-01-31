package dev.sharanggupta.loan.service;

import dev.sharanggupta.loan.dto.LoanDto;

public interface LoanService {
    // Workflow test scenario 1: PATCH version bump

    void createLoan(LoanDto loanDto);

    LoanDto fetchLoan(String mobileNumber);

    void updateLoan(LoanDto loanDto);

    void deleteLoan(String mobileNumber);
}