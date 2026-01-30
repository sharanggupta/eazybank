package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Schema(name = "ErrorResponse", description = "Standard error response returned on failure")
public record ErrorResponseDto(
        @Schema(description = "API path that caused the error", example = "/api/customers/1234567890")
        String apiPath,

        @Schema(description = "HTTP status code", example = "NOT_FOUND")
        HttpStatus errorCode,

        @Schema(description = "Error message describing what went wrong", example = "No account found for mobile number: 1234567890")
        String errorMessage,

        @Schema(description = "Timestamp when the error occurred", example = "2025-01-15T10:30:00")
        LocalDateTime errorTimestamp
) {}
