package dev.sharanggupta.customergateway.service;

import dev.sharanggupta.customergateway.dto.CustomerAccount;
import reactor.core.publisher.Mono;

public interface AccountService {
    Mono<Void> createAccount(CustomerAccount customerAccount);

    Mono<CustomerAccount> fetchAccountDetails(String mobileNumber);

    Mono<Void> updateAccount(CustomerAccount customerAccount);

    Mono<Void> deleteAccount(String mobileNumber);
}
