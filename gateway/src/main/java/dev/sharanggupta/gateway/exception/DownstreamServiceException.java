package dev.sharanggupta.gateway.exception;

public abstract class DownstreamServiceException extends RuntimeException {

    protected DownstreamServiceException(String serviceName, String message) {
        super(serviceName + " service error: " + message);
    }
}
