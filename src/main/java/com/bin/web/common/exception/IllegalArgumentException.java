package com.bin.web.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class IllegalArgumentException extends RuntimeException {

    private final String messageKey;
    private final HttpStatus status;

    public IllegalArgumentException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
        this.status = HttpStatus.BAD_REQUEST;
    }
}
