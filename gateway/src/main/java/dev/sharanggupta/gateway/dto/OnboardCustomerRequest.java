package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "OnboardCustomerRequest", description = "Request payload for onboarding a new customer")
public record OnboardCustomerRequest(
        @Schema(description = "Full name of the customer", example = "John Doe")
        @NotBlank @Size(min = 5, max = 30)
        String name,

        @Schema(description = "Email address of the customer", example = "john.doe@example.com")
        @NotBlank @Email
        String email,

        @Schema(description = "10-digit mobile number", example = "1234567890")
        @NotBlank @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
        String mobileNumber
) {}
