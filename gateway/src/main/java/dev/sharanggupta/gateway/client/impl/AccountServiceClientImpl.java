package dev.sharanggupta.gateway.client.impl;

import dev.sharanggupta.gateway.client.AccountServiceClient;
import dev.sharanggupta.gateway.client.dto.AccountRequest;
import dev.sharanggupta.gateway.client.dto.AccountResponse;
import dev.sharanggupta.gateway.dto.AccountInfoDto;
import dev.sharanggupta.gateway.exception.AccountServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
@Slf4j
public class AccountServiceClientImpl implements AccountServiceClient {

    private final RestClient restClient;
    private final String accountServiceUrl;

    public AccountServiceClientImpl(RestClient restClient,
                                    @Value("${services.account-url}") String accountServiceUrl) {
        this.restClient = restClient;
        this.accountServiceUrl = accountServiceUrl;
    }

    @Override
    public Optional<AccountInfoDto> fetchAccountByMobileNumber(String mobileNumber) {
        log.debug("Fetching account for mobile number: {}", mobileNumber);

        try {
            return restClient.get()
                    .uri(accountServiceUrl + "/account/api/fetch?mobileNumber={mobileNumber}", mobileNumber)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is4xxClientError()) {
                            log.debug("Account not found for mobile number: {}", mobileNumber);
                            return Optional.<AccountInfoDto>empty();
                        }
                        if (response.getStatusCode().isError()) {
                            throw new AccountServiceException("returned " + response.getStatusCode());
                        }
                        AccountResponse body = response.bodyTo(AccountResponse.class);
                        AccountResponse.AccountDto account = body.accountDto();
                        return Optional.of(AccountInfoDto.builder()
                                .name(body.name())
                                .email(body.email())
                                .accountNumber(account != null ? account.accountNumber() : null)
                                .accountType(account != null ? account.accountType() : null)
                                .branchAddress(account != null ? account.branchAddress() : null)
                                .build());
                    });
        } catch (AccountServiceException e) {
            throw e;
        } catch (RestClientException e) {
            throw new AccountServiceException(e.getMessage());
        }
    }

    @Override
    public void createAccount(String name, String email, String mobileNumber) {
        log.debug("Creating account for mobile number: {}", mobileNumber);

        try {
            restClient.post()
                    .uri(accountServiceUrl + "/account/api/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AccountRequest(name, email, mobileNumber, null))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RestClientException e) {
            throw new AccountServiceException(e.getMessage());
        }
    }

    @Override
    public void updateAccount(String mobileNumber, AccountInfoDto updatedAccount) {
        log.debug("Updating account for mobile number: {}", mobileNumber);

        AccountRequest.AccountDto accountDto = new AccountRequest.AccountDto(
                updatedAccount.accountNumber(),
                updatedAccount.accountType(),
                updatedAccount.branchAddress()
        );

        try {
            restClient.put()
                    .uri(accountServiceUrl + "/account/api/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AccountRequest(updatedAccount.name(), updatedAccount.email(), mobileNumber, accountDto))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RestClientException e) {
            throw new AccountServiceException(e.getMessage());
        }
    }

    @Override
    public void deleteAccount(String mobileNumber) {
        log.debug("Deleting account for mobile number: {}", mobileNumber);

        try {
            restClient.delete()
                    .uri(accountServiceUrl + "/account/api/delete?mobileNumber={mobileNumber}", mobileNumber)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new AccountServiceException("returned " + response.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (AccountServiceException e) {
            throw e;
        } catch (RestClientException e) {
            throw new AccountServiceException(e.getMessage());
        }
    }
}
