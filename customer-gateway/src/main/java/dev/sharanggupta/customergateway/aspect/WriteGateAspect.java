package dev.sharanggupta.customergateway.aspect;

import dev.sharanggupta.customergateway.annotation.ProtectedWrite;
import dev.sharanggupta.customergateway.service.WriteGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * AOP Aspect that enforces the Write Gate pattern.
 *
 * Intercepts all methods annotated with @ProtectedWrite.
 * Before method execution, checks if any circuit breaker is OPEN.
 *
 * If any CB is OPEN or HALF_OPEN → Blocks operation with 503 Service Unavailable
 * If all CB are CLOSED → Allows operation to proceed normally
 *
 * This prevents partial writes when downstream services are degraded.
 *
 * @see dev.sharanggupta.customergateway.annotation.ProtectedWrite
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class WriteGateAspect {

    private final WriteGate writeGate;

    @Around("@annotation(dev.sharanggupta.customergateway.annotation.ProtectedWrite)")
    public Object enforceWriteGate(ProceedingJoinPoint joinPoint) throws Throwable {
        log.debug("WriteGate protecting write operation: {}", joinPoint.getSignature().getName());

        return writeGate.checkWriteAllowed()
                .then(Mono.defer(() -> {
                    try {
                        return (Mono<?>) joinPoint.proceed();
                    } catch (Throwable e) {
                        return Mono.error(e);
                    }
                }))
                .cast(Object.class);
    }
}