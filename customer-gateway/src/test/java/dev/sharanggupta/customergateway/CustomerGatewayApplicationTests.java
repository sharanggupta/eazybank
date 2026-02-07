package dev.sharanggupta.customergateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class CustomerGatewayApplicationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Provide dummy URLs for service configuration
        registry.add("services.account-url", () -> "http://localhost:8080");
        registry.add("services.card-url", () -> "http://localhost:9000");
        registry.add("services.loan-url", () -> "http://localhost:8090");
    }

    @Test
    void contextLoads() {
    }
}