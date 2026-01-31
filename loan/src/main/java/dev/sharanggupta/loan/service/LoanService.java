package dev.sharanggupta.loan.service;

import dev.sharanggupta.loan.dto.LoanDto;

public interface LoanService {

    void createLoan(LoanDto loanDto);

    LoanDto fetchLoan(String mobileNumber);

    void updateLoan(LoanDto loanDto);

    void deleteLoan(String mobileNumber);
}