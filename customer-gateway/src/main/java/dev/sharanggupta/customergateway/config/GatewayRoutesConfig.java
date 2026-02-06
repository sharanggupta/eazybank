package dev.sharanggupta.customergateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    private static final String CARD_ROUTE = "card-route";
    private static final String LOAN_ROUTE = "loan-route";
    private static final String CARD_PATH_PATTERN = "/api/customer/{mobileNumber}/card/**";
    private static final String LOAN_PATH_PATTERN = "/api/customer/{mobileNumber}/loan/**";
    private static final String CARD_REWRITE_PATTERN = "/api/customer/(?<mobile>[^/]+)/card/(?<segment>/?.*)";
    private static final String LOAN_REWRITE_PATTERN = "/api/customer/(?<mobile>[^/]+)/loan/(?<segment>/?.*)";
    private static final String CARD_REWRITE_REPLACEMENT = "/card/api/${mobile}/${segment}";
    private static final String LOAN_REWRITE_REPLACEMENT = "/loan/api/${mobile}/${segment}";
    private static final String CARD_CB_NAME = "card_service";
    private static final String LOAN_CB_NAME = "loan_service";

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder, ServiceProperties properties) {
        return builder.routes()
                .route(CARD_ROUTE, r -> r
                        .path(CARD_PATH_PATTERN)
                        .filters(f -> f
                                .rewritePath(CARD_REWRITE_PATTERN, CARD_REWRITE_REPLACEMENT)
                                .circuitBreaker(config -> config.setName(CARD_CB_NAME)))
                        .uri(properties.cardUrl()))
                .route(LOAN_ROUTE, r -> r
                        .path(LOAN_PATH_PATTERN)
                        .filters(f -> f
                                .rewritePath(LOAN_REWRITE_PATTERN, LOAN_REWRITE_REPLACEMENT)
                                .circuitBreaker(config -> config.setName(LOAN_CB_NAME)))
                        .uri(properties.loanUrl()))
                .build();
    }
}
