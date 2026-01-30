package dev.sharanggupta.gateway.exception;

public class AccountServiceException extends DownstreamServiceException {

    public AccountServiceException(String message) {
        super("Account", message);
    }
}
