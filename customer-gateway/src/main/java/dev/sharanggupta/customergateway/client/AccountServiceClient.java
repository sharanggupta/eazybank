package dev.sharanggupta.customergateway.client;

import dev.sharanggupta.customergateway.config.ServiceProperties;
import dev.sharanggupta.customergateway.dto.CustomerAccount;
import dev.sharanggupta.customergateway.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AccountServiceClient {

    private static final String BASE_URI = "account/api";
    private static final String MOBILE_URI = "account/api/{mobileNumber}";

    private final WebClient webClient;

    public AccountServiceClient(WebClient.Builder builder, ServiceProperties props) {
        this.webClient = builder.baseUrl(props.accountUrl()).build();
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
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.debug("Account not found for mobile: {}", mobileNumber);
                    return Mono.error(new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));
                })
                .doOnError(e -> {
                    if (!(e instanceof ResourceNotFoundException)) {
                        log.error("Error fetching account for mobile: {}", mobileNumber, e);
                    }
                });
    }

    public Mono<Void> updateAccount(CustomerAccount customerAccount) {
        return webClient.put()
                .uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(customerAccount)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.debug("Account not found for update, mobile: {}", customerAccount.mobileNumber());
                    return Mono.error(new ResourceNotFoundException("Account", "mobileNumber", customerAccount.mobileNumber()));
                })
                .doOnError(e -> {
                    if (!(e instanceof ResourceNotFoundException)) {
                        log.error("Error updating account for mobile: {}", customerAccount.mobileNumber(), e);
                    }
                });
    }

    public Mono<Void> deleteAccount(String mobileNumber) {
        return webClient.delete()
                .uri(MOBILE_URI, mobileNumber)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.debug("Account not found for delete, mobile: {}", mobileNumber);
                    return Mono.error(new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));
                })
                .doOnError(e -> {
                    if (!(e instanceof ResourceNotFoundException)) {
                        log.error("Error deleting account for mobile: {}", mobileNumber, e);
                    }
                });
    }
}
