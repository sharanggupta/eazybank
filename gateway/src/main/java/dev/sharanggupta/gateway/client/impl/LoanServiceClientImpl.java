package dev.sharanggupta.gateway.client.impl;

import dev.sharanggupta.gateway.client.LoanServiceClient;
import dev.sharanggupta.gateway.client.dto.LoanRequest;
import dev.sharanggupta.gateway.client.dto.LoanResponse;
import dev.sharanggupta.gateway.dto.LoanInfoDto;
import dev.sharanggupta.gateway.exception.LoanServiceException;
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
public class LoanServiceClientImpl implements LoanServiceClient {

    private final RestClient restClient;
    private final String loanServiceUrl;

    public LoanServiceClientImpl(RestClient restClient,
                                 @Value("${services.loan-url}") String loanServiceUrl) {
        this.restClient = restClient;
        this.loanServiceUrl = loanServiceUrl;
    }

    @Override
    public Optional<LoanInfoDto> fetchLoanByMobileNumber(String mobileNumber) {
        log.debug("Fetching loan for mobile number: {}", mobileNumber);

        try {
            return restClient.get()
                    .uri(loanServiceUrl + "/loan/api?mobileNumber={mobileNumber}", mobileNumber)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is4xxClientError()) {
                            log.debug("No loan found for mobile number: {}", mobileNumber);
                            return Optional.<LoanInfoDto>empty();
                        }
                        if (response.getStatusCode().isError()) {
                            throw new LoanServiceException("returned " + response.getStatusCode());
                        }
                        LoanResponse body = response.bodyTo(LoanResponse.class);
                        return Optional.of(new LoanInfoDto(
                                body.loanNumber(), body.loanType(),
                                body.totalLoan(), body.amountPaid(), body.outstandingAmount()));
                    });
        } catch (LoanServiceException e) {
            throw e;
        } catch (RestClientException e) {
            throw new LoanServiceException(e.getMessage());
        }
    }

    @Override
    public void createLoan(String mobileNumber, String loanType, int totalLoan) {
        log.debug("Creating loan for mobile number: {}", mobileNumber);

        try {
            restClient.post()
                    .uri(loanServiceUrl + "/loan/api")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LoanRequest(mobileNumber, null, loanType, totalLoan, 0))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RestClientException e) {
            throw new LoanServiceException(e.getMessage());
        }
    }

    @Override
    public void updateLoan(String mobileNumber, String loanType, int totalLoan, int amountPaid) {
        log.debug("Updating loan for mobile number: {}", mobileNumber);

        try {
            restClient.put()
                    .uri(loanServiceUrl + "/loan/api")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LoanRequest(mobileNumber, null, loanType, totalLoan, amountPaid))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RestClientException e) {
            throw new LoanServiceException(e.getMessage());
        }
    }

    @Override
    public void deleteLoan(String mobileNumber) {
        log.debug("Deleting loan for mobile number: {}", mobileNumber);

        try {
            restClient.delete()
                    .uri(loanServiceUrl + "/loan/api?mobileNumber={mobileNumber}", mobileNumber)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) ->
                            log.debug("Loan not found for mobile number: {}", mobileNumber))
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new LoanServiceException("returned " + response.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (LoanServiceException e) {
            throw e;
        } catch (RestClientException e) {
            throw new LoanServiceException(e.getMessage());
        }
    }
}
