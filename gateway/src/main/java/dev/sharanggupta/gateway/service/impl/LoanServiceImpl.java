package dev.sharanggupta.gateway.service.impl;

import dev.sharanggupta.gateway.client.LoanServiceClient;
import dev.sharanggupta.gateway.dto.CreateLoanRequest;
import dev.sharanggupta.gateway.dto.LoanInfoDto;
import dev.sharanggupta.gateway.dto.UpdateLoanRequest;
import dev.sharanggupta.gateway.service.LoanService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    private static final String CIRCUIT_BREAKER = "loan-service";

    private final LoanServiceClient loanClient;

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "applyForLoanFallback")
    public void applyForLoan(String mobileNumber, CreateLoanRequest request) {
        log.info("Applying for loan for mobile number: {}", mobileNumber);
        loanClient.createLoan(mobileNumber, request.loanType(), request.totalLoan());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "getLoanFallback")
    public Optional<LoanInfoDto> getLoan(String mobileNumber) {
        log.info("Fetching loan for mobile number: {}", mobileNumber);
        return loanClient.fetchLoanByMobileNumber(mobileNumber);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "updateLoanFallback")
    public void updateLoan(String mobileNumber, UpdateLoanRequest request) {
        log.info("Updating loan for mobile number: {}", mobileNumber);
        loanClient.updateLoan(mobileNumber, request.loanType(), request.totalLoan(), request.amountPaid());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "closeLoanFallback")
    public void closeLoan(String mobileNumber) {
        log.info("Closing loan for mobile number: {}", mobileNumber);
        loanClient.deleteLoan(mobileNumber);
    }

    private Optional<LoanInfoDto> getLoanFallback(String mobileNumber, Exception cause) {
        log.warn("Loan service unavailable for {}", mobileNumber, cause);
        return Optional.empty();
    }

    private void applyForLoanFallback(String mobileNumber, CreateLoanRequest request, Exception cause) {
        log.warn("Loan service unavailable for applying loan to {}", mobileNumber, cause);
        // Circuit breaker will throw exception to WriteGateInterceptor
    }

    private void updateLoanFallback(String mobileNumber, UpdateLoanRequest request, Exception cause) {
        log.warn("Loan service unavailable for updating loan of {}", mobileNumber, cause);
        // Circuit breaker will throw exception to WriteGateInterceptor
    }

    private void closeLoanFallback(String mobileNumber, Exception cause) {
        log.warn("Loan service unavailable for closing loan of {}", mobileNumber, cause);
        // Circuit breaker will throw exception to WriteGateInterceptor
    }
}