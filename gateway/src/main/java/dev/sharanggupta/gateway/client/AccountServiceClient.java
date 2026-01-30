package dev.sharanggupta.gateway.client;

import dev.sharanggupta.gateway.dto.AccountInfoDto;

import java.util.Optional;

public interface AccountServiceClient {

    Optional<AccountInfoDto> fetchAccountByMobileNumber(String mobileNumber);

    void createAccount(String name, String email, String mobileNumber);

    void updateAccount(String mobileNumber, AccountInfoDto updatedAccount);

    void deleteAccount(String mobileNumber);
}