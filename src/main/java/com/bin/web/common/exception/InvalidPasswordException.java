package com.bin.web.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidPasswordException extends RuntimeException {

    private final String messageKey;
    private final HttpStatus status;

    public InvalidPasswordException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
        this.status = HttpStatus.FORBIDDEN;
    }
}
