package dev.sharanggupta.customergateway.service;

import dev.sharanggupta.customergateway.client.AccountServiceClient;
import dev.sharanggupta.customergateway.dto.CustomerAccount;
import dev.sharanggupta.customergateway.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private static final String CIRCUIT_BREAKER_NAME = "account_service";

    private final AccountServiceClient accountServiceClient;

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackCreateAccount")
    public Mono<Void> createAccount(CustomerAccount customerAccount) {
        log.debug("Creating account for customer: {}", customerAccount.mobileNumber());
        return accountServiceClient.createAccount(customerAccount);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFetchAccountDetails")
    public Mono<CustomerAccount> fetchAccountDetails(String mobileNumber) {
        log.debug("Fetching account details for mobile: {}", mobileNumber);
        return accountServiceClient.fetchAccount(mobileNumber);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackUpdateAccount")
    public Mono<Void> updateAccount(CustomerAccount customerAccount) {
        log.debug("Updating account for customer: {}", customerAccount.mobileNumber());
        return accountServiceClient.updateAccount(customerAccount);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackDeleteAccount")
    public Mono<Void> deleteAccount(String mobileNumber) {
        log.debug("Deleting account for mobile: {}", mobileNumber);
        return accountServiceClient.deleteAccount(mobileNumber);
    }

    // ========== Fallback Methods ==========
    // Account service is critical - fallbacks return errors rather than empty values

    private Mono<Void> fallbackCreateAccount(CustomerAccount customerAccount, Throwable throwable) {
        log.error("Account service unavailable for create, mobile: {}", customerAccount.mobileNumber(), throwable);
        return Mono.error(new ServiceUnavailableException(
                "Account service is currently unavailable. Please try again later."));
    }

    private Mono<CustomerAccount> fallbackFetchAccountDetails(String mobileNumber, Throwable throwable) {
        log.error("Account service unavailable for fetch, mobile: {}", mobileNumber, throwable);
        return Mono.error(new ServiceUnavailableException(
                "Account service is currently unavailable. Please try again later."));
    }

    private Mono<Void> fallbackUpdateAccount(CustomerAccount customerAccount, Throwable throwable) {
        log.error("Account service unavailable for update, mobile: {}", customerAccount.mobileNumber(), throwable);
        return Mono.error(new ServiceUnavailableException(
                "Account service is currently unavailable. Please try again later."));
    }

    private Mono<Void> fallbackDeleteAccount(String mobileNumber, Throwable throwable) {
        log.error("Account service unavailable for delete, mobile: {}", mobileNumber, throwable);
        return Mono.error(new ServiceUnavailableException(
                "Account service is currently unavailable. Please try again later."));
    }
}
