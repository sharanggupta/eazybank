package dev.sharanggupta.gateway.service.impl;

import dev.sharanggupta.gateway.client.LoanServiceClient;
import dev.sharanggupta.gateway.dto.CreateLoanRequest;
import dev.sharanggupta.gateway.dto.LoanInfoDto;
import dev.sharanggupta.gateway.dto.UpdateLoanRequest;
import dev.sharanggupta.gateway.exception.ResourceNotFoundException;
import dev.sharanggupta.gateway.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final LoanServiceClient loanClient;

    @Override
    public void applyForLoan(String mobileNumber, CreateLoanRequest request) {
        log.info("Creating loan for mobile number: {}", mobileNumber);
        loanClient.createLoan(mobileNumber, request.loanType(), request.totalLoan());
    }

    @Override
    public LoanInfoDto getLoan(String mobileNumber) {
        log.info("Fetching loan for mobile number: {}", mobileNumber);
        return loanClient.fetchLoanByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "mobileNumber", mobileNumber));
    }

    @Override
    public void updateLoan(String mobileNumber, UpdateLoanRequest request) {
        log.info("Updating loan for mobile number: {}", mobileNumber);
        loanClient.updateLoan(mobileNumber, request.loanType(), request.totalLoan(), request.amountPaid());
    }

    @Override
    public void closeLoan(String mobileNumber) {
        log.info("Closing loan for mobile number: {}", mobileNumber);
        loanClient.deleteLoan(mobileNumber);
    }
}
