package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateProfileRequest", description = "Request payload for updating a customer's profile")
public record UpdateProfileRequest(
        @Schema(description = "Updated full name of the customer", example = "Jane Doe")
        @NotBlank @Size(min = 5, max = 30)
        String name,

        @Schema(description = "Updated email address", example = "jane.doe@example.com")
        @NotBlank @Email
        String email
) {}
