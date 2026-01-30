package dev.sharanggupta.gateway.service.impl;

import dev.sharanggupta.gateway.client.AccountServiceClient;
import dev.sharanggupta.gateway.dto.AccountInfoDto;
import dev.sharanggupta.gateway.dto.CardInfoDto;
import dev.sharanggupta.gateway.dto.CustomerDetailsDto;
import dev.sharanggupta.gateway.dto.LoanInfoDto;
import dev.sharanggupta.gateway.dto.OnboardCustomerRequest;
import dev.sharanggupta.gateway.dto.UpdateProfileRequest;
import dev.sharanggupta.gateway.exception.DownstreamServiceException;
import dev.sharanggupta.gateway.exception.ResourceNotFoundException;
import dev.sharanggupta.gateway.service.CardService;
import dev.sharanggupta.gateway.service.CustomerService;
import dev.sharanggupta.gateway.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final AccountServiceClient accountClient;
    private final CardService cardService;
    private final LoanService loanService;

    @Override
    public CustomerDetailsDto getCustomerDetails(String mobileNumber) {
        log.info("Fetching customer details for mobile number: {}", mobileNumber);

        AccountInfoDto account = accountClient.fetchAccountByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));

        return CustomerDetailsDto.builder()
                .mobileNumber(mobileNumber)
                .account(account)
                .card(fetchCard(mobileNumber))
                .loan(fetchLoan(mobileNumber))
                .build();
    }

    @Override
    public void onboardCustomer(OnboardCustomerRequest request) {
        log.info("Onboarding customer with mobile number: {}", request.mobileNumber());
        accountClient.createAccount(request.name(), request.email(), request.mobileNumber());
    }

    @Override
    public void updateProfile(String mobileNumber, UpdateProfileRequest request) {
        log.info("Updating profile for mobile number: {}", mobileNumber);

        AccountInfoDto existingAccount = accountClient.fetchAccountByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));

        AccountInfoDto updatedAccount = AccountInfoDto.builder()
                .name(request.name())
                .email(request.email())
                .accountNumber(existingAccount.accountNumber())
                .accountType(existingAccount.accountType())
                .branchAddress(existingAccount.branchAddress())
                .build();

        accountClient.updateAccount(mobileNumber, updatedAccount);
    }

    @Override
    public void offboardCustomer(String mobileNumber) {
        log.info("Offboarding customer with mobile number: {}", mobileNumber);

        try {
            cardService.cancelCard(mobileNumber);
        } catch (DownstreamServiceException e) {
            log.warn("Failed to delete card for mobile number {}: {}", mobileNumber, e.getMessage());
        }

        try {
            loanService.closeLoan(mobileNumber);
        } catch (DownstreamServiceException e) {
            log.warn("Failed to delete loan for mobile number {}: {}", mobileNumber, e.getMessage());
        }

        accountClient.deleteAccount(mobileNumber);
    }

    private CardInfoDto fetchCard(String mobileNumber) {
        try {
            return cardService.getCard(mobileNumber);
        } catch (ResourceNotFoundException | DownstreamServiceException e) {
            log.warn("Card unavailable for {}: {}", mobileNumber, e.getMessage());
            return null;
        }
    }

    private LoanInfoDto fetchLoan(String mobileNumber) {
        try {
            return loanService.getLoan(mobileNumber);
        } catch (ResourceNotFoundException | DownstreamServiceException e) {
            log.warn("Loan unavailable for {}: {}", mobileNumber, e.getMessage());
            return null;
        }
    }
}
