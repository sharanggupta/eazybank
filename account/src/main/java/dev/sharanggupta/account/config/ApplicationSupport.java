package dev.sharanggupta.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.support")
public record ApplicationSupport(
        Contact contact,
        String message,
        List<String> oncallsupport
) {
    public record Contact(
            String name,
            String email
    ) {
    }
}