package dev.sharanggupta.gateway.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s not found for %s: %s", resourceName, fieldName, fieldValue));
    }
}
