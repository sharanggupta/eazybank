package dev.sharanggupta.customergateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway route configuration for proxying requests to downstream services.
 *
 * Customer-centric nested resource routes (RESTful sub-resources):
 *   - /api/customer/{mobileNumber}/card   → /card/api/{mobileNumber} (POST, GET, PUT, DELETE)
 *   - /api/customer/{mobileNumber}/loan   → /loan/api/{mobileNumber} (POST, GET, PUT, DELETE)
 *   - /account/**                         → account service (direct proxy)
 *
 * This provides a unified API where cards and loans are accessed as
 * sub-resources of a customer identified by mobile number.
 */
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder, ServiceProperties properties) {
        return builder.routes()

                // Card as a sub-resource of customer
                .route("customer-card-route", r -> r
                        .path("/api/customer/{mobileNumber}/card/**")
                        .filters(f -> f.rewritePath(
                                "/api/customer/(?<mobile>[^/]+)/card(?<segment>/?.*)",
                                "/card/api/${mobile}${segment}"))
                        .uri(properties.cardUrl()))

                // Loan as a sub-resource of customer
                .route("customer-loan-route", r -> r
                        .path("/api/customer/{mobileNumber}/loan/**")
                        .filters(f -> f.rewritePath(
                                "/api/customer/(?<mobile>[^/]+)/loan(?<segment>/?.*)",
                                "/loan/api/${mobile}${segment}"))
                        .uri(properties.loanUrl()))

                // Account service proxy (not nested under customer)
                .route("account-proxy", r -> r
                        .path("/account/**")
                        .uri(properties.accountUrl()))

                .build();
    }
}
