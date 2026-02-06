package dev.sharanggupta.customergateway.service;

import dev.sharanggupta.customergateway.dto.LoanInfo;
import reactor.core.publisher.Mono;

public interface LoanService {
    Mono<LoanInfo> fetchLoan(String mobileNumber);

    Mono<Void> deleteLoan(String mobileNumber);
}
