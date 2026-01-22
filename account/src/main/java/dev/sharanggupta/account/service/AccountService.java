package dev.sharanggupta.account.service;

import dev.sharanggupta.account.dto.CustomerDto;

public interface AccountService {
/**
*
 * @param customerDto encapsulates customer information captured
*/
    void createAccount(CustomerDto customerDto);

    CustomerDto fetchAccountDetails(String mobileNumber);

    void updateAccount(CustomerDto customerDto);

    void deleteAccount(String mobileNumber);
}
