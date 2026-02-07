package dev.sharanggupta.customergateway.service;

import dev.sharanggupta.customergateway.annotation.ProtectedWrite;
import dev.sharanggupta.customergateway.dto.CardInfo;
import dev.sharanggupta.customergateway.dto.CustomerAccount;
import dev.sharanggupta.customergateway.dto.CustomerProfile;
import dev.sharanggupta.customergateway.dto.LoanInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Customer Service - Orchestrates account, card, and loan services.
 *
 * Write operations (marked with @ProtectedWrite) are automatically protected
 * by the WriteGateAspect, which checks if any circuit breaker is OPEN before
 * allowing the operation to proceed.
 *
 * @see dev.sharanggupta.customergateway.aspect.WriteGateAspect
 * @see dev.sharanggupta.customergateway.annotation.ProtectedWrite
 */
@Service
@AllArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final AccountService accountService;
    private final CardService cardService;
    private final LoanService loanService;

    // ========== Write Operations (Protected) ==========

    @Override
    @ProtectedWrite
    public Mono<Void> onboardCustomer(CustomerAccount customerAccount) {
        log.info("Onboarding customer with mobile: {}", customerAccount.mobileNumber());
        return accountService.createAccount(customerAccount);
    }

    @Override
    @ProtectedWrite
    public Mono<Void> updateCustomer(CustomerAccount customerAccount) {
        log.info("Updating customer details for mobile: {}", customerAccount.mobileNumber());
        return accountService.updateAccount(customerAccount);
    }

    @Override
    @ProtectedWrite
    public Mono<Void> offboardCustomer(String mobileNumber) {
        log.info("Offboarding customer with mobile: {}", mobileNumber);
        return cardService.deleteCard(mobileNumber)
                .then(loanService.deleteLoan(mobileNumber))
                .then(accountService.deleteAccount(mobileNumber));
    }

    // ========== Read Operations ==========

    @Override
    public Mono<CustomerProfile> getCustomerDetails(String mobileNumber) {
        log.info("Fetching customer details for mobile: {}", mobileNumber);
        return accountService.fetchAccountDetails(mobileNumber)
                .flatMap(customerAccount -> enrichWithCardAndLoan(customerAccount, mobileNumber));
    }

    // ========== Internal Helpers ==========

    private Mono<CustomerProfile> enrichWithCardAndLoan(CustomerAccount customerAccount, String mobileNumber) {
        CustomerProfile baseProfile = CustomerProfile.builder()
                .name(customerAccount.name())
                .email(customerAccount.email())
                .mobileNumber(customerAccount.mobileNumber())
                .account(customerAccount.account())
                .build();

        Mono<Optional<CardInfo>> cardMono = cardService.fetchCard(mobileNumber)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());

        Mono<Optional<LoanInfo>> loanMono = loanService.fetchLoan(mobileNumber)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());

        return Mono.zip(cardMono, loanMono)
                .map(tuple -> baseProfile.toBuilder()
                        .card(tuple.getT1().orElse(null))
                        .loan(tuple.getT2().orElse(null))
                        .build());
    }
}
