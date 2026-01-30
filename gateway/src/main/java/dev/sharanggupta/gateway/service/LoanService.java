package dev.sharanggupta.gateway.service;

import dev.sharanggupta.gateway.dto.CreateLoanRequest;
import dev.sharanggupta.gateway.dto.LoanInfoDto;
import dev.sharanggupta.gateway.dto.UpdateLoanRequest;

public interface LoanService {

    void applyForLoan(String mobileNumber, CreateLoanRequest request);

    LoanInfoDto getLoan(String mobileNumber);

    void updateLoan(String mobileNumber, UpdateLoanRequest request);

    void closeLoan(String mobileNumber);
}
