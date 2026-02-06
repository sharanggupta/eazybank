package dev.sharanggupta.customergateway.client;

import dev.sharanggupta.customergateway.dto.CustomerAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AccountServiceClient {

    private static final String BASE_URI = "account/api";
    private static final String MOBILE_URI = "account/api/{mobileNumber}";

    private final WebClient webClient;

    public AccountServiceClient(@Qualifier("account") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Void> createAccount(CustomerAccount customerAccount) {
        return webClient.post()
                .uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(customerAccount)
                .retrieve()
                .toBodilessEntity()
                .then()
                .doOnError(e -> log.error("Error creating account for mobile: {}", customerAccount.mobileNumber(), e));
    }

    public Mono<CustomerAccount> fetchAccount(String mobileNumber) {
        return webClient.get()
                .uri(MOBILE_URI, mobileNumber)
                .retrieve()
                .bodyToMono(CustomerAccount.class)
                .doOnError(e -> log.error("Error fetching account for mobile: {}", mobileNumber, e));
    }

    public Mono<Void> updateAccount(CustomerAccount customerAccount) {
        return webClient.put()
                .uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(customerAccount)
                .retrieve()
                .toBodilessEntity()
                .then()
                .doOnError(e -> log.error("Error updating account for mobile: {}", customerAccount.mobileNumber(), e));
    }

    public Mono<Void> deleteAccount(String mobileNumber) {
        return webClient.delete()
                .uri(MOBILE_URI, mobileNumber)
                .retrieve()
                .toBodilessEntity()
                .then()
                .doOnError(e -> log.error("Error deleting account for mobile: {}", mobileNumber, e));
    }
}
