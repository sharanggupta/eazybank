package dev.sharanggupta.loan.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;

@Configuration
public class ObservabilityConfig {

    private static final String ACTUATOR_PATH = "/loan/actuator";

    @Bean
    ObservationPredicate excludeActuatorFromTracing() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext ctx) {
                return !ctx.getCarrier().getPath().value().startsWith(ACTUATOR_PATH);
            }
            return true;
        };
    }
}
