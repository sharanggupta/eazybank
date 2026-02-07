package dev.sharanggupta.customergateway.service;

import dev.sharanggupta.customergateway.client.LoanServiceClient;
import dev.sharanggupta.customergateway.dto.LoanInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    private static final String CIRCUIT_BREAKER_NAME = "loan_service";

    private final LoanServiceClient loanServiceClient;

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFetchLoan")
    public Mono<LoanInfo> fetchLoan(String mobileNumber) {
        log.debug("Fetching loan for mobile: {}", mobileNumber);
        return loanServiceClient.fetchLoan(mobileNumber);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackDeleteLoan")
    public Mono<Void> deleteLoan(String mobileNumber) {
        log.debug("Deleting loan for mobile: {}", mobileNumber);
        return loanServiceClient.deleteLoan(mobileNumber);
    }

    private Mono<LoanInfo> fallbackFetchLoan(String mobileNumber, Throwable throwable) {
        log.warn("Loan service unavailable for mobile: {}", mobileNumber, throwable);
        return Mono.empty();
    }

    private Mono<Void> fallbackDeleteLoan(String mobileNumber, Throwable throwable) {
        log.warn("Loan service unavailable for delete, mobile: {}", mobileNumber, throwable);
        return Mono.empty();
    }
}
