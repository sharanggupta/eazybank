package dev.sharanggupta.account.service;

import dev.sharanggupta.account.dto.CustomerDto;
import reactor.core.publisher.Mono;

public interface AccountService {
/**
*
 * @param customerDto encapsulates customer information captured
*/
    Mono<Void> createAccount(CustomerDto customerDto);

    Mono<CustomerDto> fetchAccountDetails(String mobileNumber);

    Mono<Void> updateAccount(CustomerDto customerDto);

    Mono<Void> deleteAccount(String mobileNumber);
}
