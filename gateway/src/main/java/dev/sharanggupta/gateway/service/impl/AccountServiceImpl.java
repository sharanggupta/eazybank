package dev.sharanggupta.gateway.service.impl;

import dev.sharanggupta.gateway.client.AccountServiceClient;
import dev.sharanggupta.gateway.dto.AccountInfoDto;
import dev.sharanggupta.gateway.service.AccountService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private static final String CIRCUIT_BREAKER = "account-service";

    private final AccountServiceClient accountClient;

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "createAccountFallback")
    public void createAccount(String name, String email, String mobileNumber) {
        log.info("Creating account for mobile number: {}", mobileNumber);
        accountClient.createAccount(name, email, mobileNumber);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "getAccountFallback")
    public Optional<AccountInfoDto> getAccount(String mobileNumber) {
        log.info("Fetching account for mobile number: {}", mobileNumber);
        return accountClient.fetchAccountByMobileNumber(mobileNumber);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "updateAccountFallback")
    public void updateAccount(String mobileNumber, AccountInfoDto updatedAccount) {
        log.info("Updating account for mobile number: {}", mobileNumber);
        accountClient.updateAccount(mobileNumber, updatedAccount);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "deleteAccountFallback")
    public void deleteAccount(String mobileNumber) {
        log.info("Deleting account for mobile number: {}", mobileNumber);
        accountClient.deleteAccount(mobileNumber);
    }

    private Optional<AccountInfoDto> getAccountFallback(String mobileNumber, Exception cause) {
        log.warn("Account service unavailable for {}", mobileNumber, cause);
        return Optional.empty();
    }

    private void createAccountFallback(String name, String email, String mobileNumber, Exception cause) {
        log.warn("Account service unavailable for creating account for {}", mobileNumber, cause);
        // Circuit breaker will throw exception to WriteGateInterceptor
    }

    private void updateAccountFallback(String mobileNumber, AccountInfoDto updatedAccount, Exception cause) {
        log.warn("Account service unavailable for updating account of {}", mobileNumber, cause);
        // Circuit breaker will throw exception to WriteGateInterceptor
    }

    private void deleteAccountFallback(String mobileNumber, Exception cause) {
        log.warn("Account service unavailable for deleting account of {}", mobileNumber, cause);
        // Circuit breaker will throw exception to WriteGateInterceptor
    }
}