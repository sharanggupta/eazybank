package dev.sharanggupta.customergateway.service;

import dev.sharanggupta.customergateway.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class WriteGateImpl implements WriteGate {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public Mono<Void> checkWriteAllowed() {
        return Mono.defer(() -> {
            Optional<String> openCircuit = findOpenCircuitBreaker();

            if (openCircuit.isPresent()) {
                String circuitName = openCircuit.get();
                String errorMessage = String.format(
                    "Write operations are blocked because the %s circuit breaker is open. " +
                    "This prevents partial updates across services. Please try again in a few moments.",
                    circuitName
                );
                log.warn("[WriteGate] Blocking write operation - circuit breaker '{}' is open", circuitName);
                return Mono.error(new ServiceUnavailableException(errorMessage));
            }
            return Mono.empty();
        });
    }

    private Optional<String> findOpenCircuitBreaker() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .filter(cb -> cb.getState() == CircuitBreaker.State.OPEN
                        || cb.getState() == CircuitBreaker.State.HALF_OPEN
                        || cb.getState() == CircuitBreaker.State.FORCED_OPEN)
                .map(CircuitBreaker::getName)
                .findFirst();
    }
}
