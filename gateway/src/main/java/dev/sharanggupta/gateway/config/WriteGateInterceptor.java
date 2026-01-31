package dev.sharanggupta.gateway.config;

import dev.sharanggupta.gateway.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WriteGateInterceptor implements HandlerInterceptor {

    private static final String HTTP_GET = "GET";
    private static final String HTTP_HEAD = "HEAD";

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isWriteRequest(request)) {
            rejectIfAnyCircuitBreakerOpen(request);
        }
        return true;
    }

    private boolean isWriteRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return !(HTTP_GET.equalsIgnoreCase(method) || HTTP_HEAD.equalsIgnoreCase(method));
    }

    private void rejectIfAnyCircuitBreakerOpen(HttpServletRequest request) {
        findOpenCircuitBreaker().ifPresent(breaker -> rejectWrite(request, breaker));
    }

    private Optional<CircuitBreaker> findOpenCircuitBreaker() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .filter(breaker -> breaker.getState() == CircuitBreaker.State.OPEN)
                .findFirst();
    }

    private void rejectWrite(HttpServletRequest request, CircuitBreaker breaker) {
        log.warn("Rejecting {} {} — circuit breaker '{}' is open",
                request.getMethod(), request.getRequestURI(), breaker.getName());
        throw new ServiceUnavailableException(
                "System is temporarily unavailable for writes — " + breaker.getName() + " is degraded");
    }
}
