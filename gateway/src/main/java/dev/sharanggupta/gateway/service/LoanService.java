package dev.sharanggupta.gateway.service;

import dev.sharanggupta.gateway.dto.CreateLoanRequest;
import dev.sharanggupta.gateway.dto.LoanInfoDto;
import dev.sharanggupta.gateway.dto.UpdateLoanRequest;

import java.util.Optional;

public interface LoanService {

    void applyForLoan(String mobileNumber, CreateLoanRequest request);

    Optional<LoanInfoDto> getLoan(String mobileNumber);

    void updateLoan(String mobileNumber, UpdateLoanRequest request);

    void closeLoan(String mobileNumber);
}
