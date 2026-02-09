package dev.sharanggupta.customergateway.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;

@Configuration
public class ObservabilityConfig {

    private static final String ACTUATOR_PATH = "/actuator";
    private static final String FAVICON_PATH = "/favicon.ico";

    @Bean
    ObservationPredicate excludeNoisyEndpointsFromTracing() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext ctx) {
                String path = ctx.getCarrier().getPath().value();
                return !path.startsWith(ACTUATOR_PATH) && !path.equals(FAVICON_PATH);
            }
            return true;
        };
    }
}
