package dev.sharanggupta.customergateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
public record ServiceProperties(
        String accountUrl,
        String cardUrl,
        String loanUrl
) {}
