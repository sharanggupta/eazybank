package dev.sharanggupta.gateway.service.impl;

import dev.sharanggupta.gateway.dto.AccountInfoDto;
import dev.sharanggupta.gateway.dto.CardInfoDto;
import dev.sharanggupta.gateway.dto.CustomerDetailsDto;
import dev.sharanggupta.gateway.dto.LoanInfoDto;
import dev.sharanggupta.gateway.dto.OnboardCustomerRequest;
import dev.sharanggupta.gateway.dto.UpdateProfileRequest;
import dev.sharanggupta.gateway.exception.DownstreamServiceException;
import dev.sharanggupta.gateway.exception.ResourceNotFoundException;
import dev.sharanggupta.gateway.service.AccountService;
import dev.sharanggupta.gateway.service.CardService;
import dev.sharanggupta.gateway.service.CustomerService;
import dev.sharanggupta.gateway.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final AccountService accountService;
    private final CardService cardService;
    private final LoanService loanService;

    @Override
    public CustomerDetailsDto getCustomerDetails(String mobileNumber) {
        log.info("Fetching customer details for mobile number: {}", mobileNumber);

        AccountInfoDto account = accountService.getAccount(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));

        var cardAndLoan = fetchCardAndLoanInParallel(mobileNumber);

        return new CustomerDetailsDto(
                mobileNumber,
                account,
                cardAndLoan.card().orElse(null),
                cardAndLoan.loan().orElse(null)
        );
    }

    @Override
    public void onboardCustomer(OnboardCustomerRequest request) {
        log.info("Onboarding customer with mobile number: {}", request.mobileNumber());
        accountService.createAccount(request.name(), request.email(), request.mobileNumber());
    }

    @Override
    public void updateProfile(String mobileNumber, UpdateProfileRequest request) {
        log.info("Updating profile for mobile number: {}", mobileNumber);

        AccountInfoDto existingAccount = accountService.getAccount(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));

        AccountInfoDto updatedAccount = applyProfileUpdate(existingAccount, request);
        accountService.updateAccount(mobileNumber, updatedAccount);
    }

    @Override
    public void offboardCustomer(String mobileNumber) {
        log.info("Offboarding customer with mobile number: {}", mobileNumber);
        tryCancelCard(mobileNumber);
        tryCloseLoan(mobileNumber);
        accountService.deleteAccount(mobileNumber);
    }

    private CardAndLoan fetchCardAndLoanInParallel(String mobileNumber) {
        var cardFuture = fetchCardGracefully(mobileNumber);
        var loanFuture = fetchLoanGracefully(mobileNumber);
        CompletableFuture.allOf(cardFuture, loanFuture).join();
        return new CardAndLoan(cardFuture.resultNow(), loanFuture.resultNow());
    }

    private CompletableFuture<Optional<CardInfoDto>> fetchCardGracefully(String mobileNumber) {
        return CompletableFuture.supplyAsync(() -> cardService.getCard(mobileNumber))
                .exceptionally(ex -> {
                    log.warn("Card fetch failed for {}: {}", mobileNumber, ex.getMessage());
                    return Optional.empty();
                });
    }

    private CompletableFuture<Optional<LoanInfoDto>> fetchLoanGracefully(String mobileNumber) {
        return CompletableFuture.supplyAsync(() -> loanService.getLoan(mobileNumber))
                .exceptionally(ex -> {
                    log.warn("Loan fetch failed for {}: {}", mobileNumber, ex.getMessage());
                    return Optional.empty();
                });
    }

    private AccountInfoDto applyProfileUpdate(AccountInfoDto existing, UpdateProfileRequest request) {
        return AccountInfoDto.builder()
                .name(request.name())
                .email(request.email())
                .accountNumber(existing.accountNumber())
                .accountType(existing.accountType())
                .branchAddress(existing.branchAddress())
                .build();
    }

    private void tryCancelCard(String mobileNumber) {
        try {
            cardService.cancelCard(mobileNumber);
        } catch (DownstreamServiceException e) {
            log.warn("Failed to cancel card for {}: {}", mobileNumber, e.getMessage());
        }
    }

    private void tryCloseLoan(String mobileNumber) {
        try {
            loanService.closeLoan(mobileNumber);
        } catch (DownstreamServiceException e) {
            log.warn("Failed to close loan for {}: {}", mobileNumber, e.getMessage());
        }
    }

    private record CardAndLoan(Optional<CardInfoDto> card, Optional<LoanInfoDto> loan) {}
}
