package dev.sharanggupta.customergateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private static final int REQUEST_TIMEOUT_MS = 5000;

    @Bean
    public WebClient account(ServiceProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.accountUrl())
                .clientConnector(createConnector())
                .build();
    }

    @Bean
    public WebClient card(ServiceProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.cardUrl())
                .clientConnector(createConnector())
                .build();
    }

    @Bean
    public WebClient loan(ServiceProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.loanUrl())
                .clientConnector(createConnector())
                .build();
    }

    private ReactorClientHttpConnector createConnector() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(REQUEST_TIMEOUT_MS));
        return new ReactorClientHttpConnector(httpClient);
    }
}
