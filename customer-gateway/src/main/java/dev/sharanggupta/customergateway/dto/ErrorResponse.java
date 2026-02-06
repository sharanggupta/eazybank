package dev.sharanggupta.customergateway.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ErrorResponse(
        String apiPath,
        HttpStatus errorCode,
        String errorMessage,
        LocalDateTime errorTimestamp
) {}
