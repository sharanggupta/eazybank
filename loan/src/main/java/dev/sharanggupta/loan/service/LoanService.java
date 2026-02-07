package dev.sharanggupta.loan.service;

import dev.sharanggupta.loan.dto.LoanCreateRequest;
import dev.sharanggupta.loan.dto.LoanDto;
import dev.sharanggupta.loan.dto.LoanUpdateRequest;
import reactor.core.publisher.Mono;

public interface LoanService {

    Mono<Void> createLoan(String mobileNumber, LoanCreateRequest request);

    Mono<LoanDto> fetchLoan(String mobileNumber);

    Mono<Void> updateLoan(String mobileNumber, LoanUpdateRequest request);

    Mono<Void> deleteLoan(String mobileNumber);
}