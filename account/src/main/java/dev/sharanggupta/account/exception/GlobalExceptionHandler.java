package dev.sharanggupta.account.exception;

import dev.sharanggupta.account.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, String>>> handleWebExchangeBindException(
            WebExchangeBindException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                String fieldName = fieldError.getField();
                String validationMsg = error.getDefaultMessage();
                validationErrors.put(fieldName, validationMsg);
            }
        });
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationErrors));
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleCustomerAlreadyExistsException(
            CustomerAlreadyExistsException exception, ServerWebExchange exchange) {
        return buildErrorResponse(exchange, HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleResourceNotFoundException(
            ResourceNotFoundException exception, ServerWebExchange exchange) {
        return buildErrorResponse(exchange, HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AccountDetailsMissingException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleAccountDetailsMissingException(
            AccountDetailsMissingException exception, ServerWebExchange exchange) {
        return buildErrorResponse(exchange, HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleGlobalException(
            Exception exception, ServerWebExchange exchange) {
        return buildErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    private Mono<ResponseEntity<ErrorResponseDto>> buildErrorResponse(
            ServerWebExchange exchange, HttpStatus status, String message) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                exchange.getRequest().getPath().value(),
                status,
                message,
                LocalDateTime.now()
        );
        return Mono.just(ResponseEntity.status(status).body(errorResponse));
    }
}
