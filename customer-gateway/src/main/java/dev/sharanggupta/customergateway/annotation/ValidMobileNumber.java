package dev.sharanggupta.customergateway.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that the annotated string is a valid 10-digit mobile number.
 *
 * This annotation consolidates mobile number validation in one place,
 * following the DRY principle.
 */
@Documented
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
@Constraint(validatedBy = {})
public @interface ValidMobileNumber {
    String message() default "Mobile number must be 10 digits";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
