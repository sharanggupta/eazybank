package dev.sharanggupta.gateway.service;

import dev.sharanggupta.gateway.dto.AccountInfoDto;
import dev.sharanggupta.gateway.dto.OnboardCustomerRequest;
import dev.sharanggupta.gateway.dto.UpdateProfileRequest;

import java.util.Optional;

public interface AccountService {

    void createAccount(String name, String email, String mobileNumber);

    Optional<AccountInfoDto> getAccount(String mobileNumber);

    void updateAccount(String mobileNumber, AccountInfoDto updatedAccount);

    void deleteAccount(String mobileNumber);
}