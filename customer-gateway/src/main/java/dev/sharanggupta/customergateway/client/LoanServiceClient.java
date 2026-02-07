package dev.sharanggupta.customergateway.client;

import dev.sharanggupta.customergateway.dto.LoanInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoanServiceClient {

    private static final String LOAN_URI = "loan/api/{mobileNumber}";

    private final WebClient webClient;

    public LoanServiceClient(@Qualifier("loan") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<LoanInfo> fetchLoan(String mobileNumber) {
        return webClient.get()
                .uri(LOAN_URI, mobileNumber)
                .retrieve()
                .bodyToMono(LoanInfo.class)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.debug("No loan found for mobile: {}", mobileNumber);
                    return Mono.empty();
                })
                .doOnError(e -> log.error("Error fetching loan for mobile: {}", mobileNumber, e));
    }

    public Mono<Void> deleteLoan(String mobileNumber) {
        return webClient.delete()
                .uri(LOAN_URI, mobileNumber)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.debug("No loan to delete for mobile: {}", mobileNumber);
                    return Mono.empty();
                })
                .doOnError(e -> log.error("Error deleting loan for mobile: {}", mobileNumber, e));
    }
}
