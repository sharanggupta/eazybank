package dev.sharanggupta.gateway.exception;

public class LoanServiceException extends DownstreamServiceException {

    public LoanServiceException(String message) {
        super("Loan", message);
    }
}
