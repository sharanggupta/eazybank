package dev.sharanggupta.gateway.exception;

import dev.sharanggupta.gateway.dto.ErrorResponseDto;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponseDto error = new ErrorResponseDto(
                request.getDescription(false),
                HttpStatus.BAD_REQUEST,
                errorMessage,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return buildErrorResponse(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpClientError(
            HttpClientErrorException ex, WebRequest request) {
        log.warn("Downstream HTTP error: {} {}", ex.getStatusCode(), ex.getMessage());
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return buildErrorResponse(request, status, ex.getStatusText());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(
            WebRequest request, HttpStatus status, String message) {
        ErrorResponseDto error = new ErrorResponseDto(
                request.getDescription(false),
                status,
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(error);
    }
}
