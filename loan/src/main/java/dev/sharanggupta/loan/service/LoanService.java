package dev.sharanggupta.loan.service;

import dev.sharanggupta.loan.dto.LoanDto;
import reactor.core.publisher.Mono;

public interface LoanService {

    Mono<Void> createLoan(LoanDto loanDto);

    Mono<LoanDto> fetchLoan(String mobileNumber);

    Mono<Void> updateLoan(LoanDto loanDto);

    Mono<Void> deleteLoan(String mobileNumber);
}