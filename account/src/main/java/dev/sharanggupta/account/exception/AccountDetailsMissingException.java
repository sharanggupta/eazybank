package dev.sharanggupta.account.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class AccountDetailsMissingException extends RuntimeException {

    public AccountDetailsMissingException(String message) {
        super(message);
    }
}
