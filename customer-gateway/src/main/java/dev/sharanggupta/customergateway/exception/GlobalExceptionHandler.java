package dev.sharanggupta.customergateway.exception;

import dev.sharanggupta.customergateway.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleWebExchangeBindException(WebExchangeBindException ex, ServerWebExchange exchange) {
        List<ObjectError> errorList = ex.getBindingResult().getAllErrors();
        String errorMessage = errorList.stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.joining(", "));

        return buildErrorResponse(exchange, HttpStatus.BAD_REQUEST, errorMessage);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, ServerWebExchange exchange) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining(", "));

        return buildErrorResponse(exchange, HttpStatus.BAD_REQUEST, errorMessage);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailableException(ServiceUnavailableException ex, ServerWebExchange exchange) {
        log.warn("Service unavailable : {}", ex.getMessage());
        return buildErrorResponse(exchange, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, ServerWebExchange exchange) {
        return buildErrorResponse(exchange, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(WebClientResponseException.BadRequest.class)
    public ResponseEntity<ErrorResponse> handleWebClientBadRequestException(WebClientResponseException.BadRequest ex, ServerWebExchange exchange) {
        return buildErrorResponse(exchange, HttpStatus.BAD_REQUEST, ex.getResponseBodyAsString());
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientResponseException(WebClientResponseException ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return buildErrorResponse(exchange, status, ex.getResponseBodyAsString());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, ServerWebExchange exchange) {
        log.error("An unexpected error occurred", ex);
        return buildErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ErrorResponse errorResponse = new ErrorResponse(
                exchange.getRequest().getPath().value(),
                status,
                message,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, status);
    }
}
