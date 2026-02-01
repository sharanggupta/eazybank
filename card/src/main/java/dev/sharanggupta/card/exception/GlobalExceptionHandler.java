package dev.sharanggupta.card.exception;

import dev.sharanggupta.card.dto.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CardAlreadyExistsException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleCardAlreadyExistsException(
            CardAlreadyExistsException ex, ServerWebExchange exchange) {

        log.warn("Card already exists: {}", ex.getMessage());
        return buildErrorResponse(exchange, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleResourceNotFoundException(
            ResourceNotFoundException ex, ServerWebExchange exchange) {

        log.info("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(exchange, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleGlobalException(
            Exception ex, ServerWebExchange exchange) {

        log.error("Unexpected error occurred for request {}: {}", exchange.getRequest().getURI(), ex.getMessage(), ex);
        return buildErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    private Mono<ResponseEntity<ErrorResponseDto>> buildErrorResponse(
            ServerWebExchange exchange, HttpStatus status, String message) {

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                exchange.getRequest().getURI().toString(),
                status,
                message,
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.status(status).body(errorResponse));
    }
}
