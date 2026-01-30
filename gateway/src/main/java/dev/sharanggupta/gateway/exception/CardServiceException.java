package dev.sharanggupta.gateway.exception;

public class CardServiceException extends DownstreamServiceException {

    public CardServiceException(String message) {
        super("Card", message);
    }
}
