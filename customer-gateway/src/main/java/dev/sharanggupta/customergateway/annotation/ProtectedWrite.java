package dev.sharanggupta.customergateway.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a write operation protected by the Write Gate pattern.
 *
 * Methods annotated with @ProtectedWrite will be blocked if any circuit breaker is OPEN,
 * preventing partial writes when the system is degraded.
 *
 * Usage:
 * <pre>
 * @ProtectedWrite
 * public Mono&lt;Void&gt; createCustomer(CustomerAccountDto dto) {
 *     // This method is protected by Write Gate
 *     // If any CB is OPEN → 503 Service Unavailable
 *     // If all CB are CLOSED → Proceed with operation
 * }
 * </pre>
 *
 * @see dev.sharanggupta.customergateway.aspect.WriteGateAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtectedWrite {
}