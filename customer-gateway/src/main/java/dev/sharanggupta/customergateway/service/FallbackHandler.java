package dev.sharanggupta.customergateway.service;

import dev.sharanggupta.customergateway.exception.ResourceNotFoundException;
import dev.sharanggupta.customergateway.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Handles fallback logic for circuit breaker protected methods.
 *
 * Circuit breakers call fallback methods for ALL exceptions, regardless of
 * ignore-exceptions configuration. This handler decides the appropriate
 * response based on exception type:
 *
 * - Business exceptions (4xx responses, ResourceNotFoundException): Propagate as-is
 * - Infrastructure exceptions (connection, timeout, 5xx): Convert to 503
 */
@Component
@Slf4j
public class FallbackHandler {

    /**
     * Handles fallback for a failed service call.
     *
     * @param throwable The exception that caused the fallback
     * @param serviceName Name of the failing service (for logging)
     * @param operation Description of the operation (for logging)
     * @return Mono.error with appropriate exception
     */
    public <T> Mono<T> handle(Throwable throwable, String serviceName, String operation) {
        // Business exceptions should propagate to the caller
        if (isBusinessException(throwable)) {
            log.debug("{} {} failed with business exception: {}", serviceName, operation, throwable.getMessage());
            return Mono.error(throwable);
        }

        // Infrastructure exceptions indicate service unavailability
        log.error("{} {} failed due to service unavailability", serviceName, operation, throwable);
        return Mono.error(new ServiceUnavailableException(
                serviceName + " is currently unavailable. Please try again later."));
    }

    /**
     * Determines if an exception represents a business error (should propagate)
     * versus an infrastructure error (should convert to 503).
     *
     * Business errors are client errors (4xx) that should be shown to the user.
     * Infrastructure errors are server/network errors that indicate service problems.
     */
    private boolean isBusinessException(Throwable throwable) {
        // Application-level business exceptions
        if (throwable instanceof ResourceNotFoundException) {
            return true;
        }

        // HTTP 4xx responses from downstream services (BadRequest, NotFound, Conflict, etc.)
        if (throwable instanceof WebClientResponseException webClientException) {
            return webClientException.getStatusCode().is4xxClientError();
        }

        return false;
    }
}