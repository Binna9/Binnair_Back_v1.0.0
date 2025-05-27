package com.bb.ballBin.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class IsActiveAccountException extends RuntimeException {

    private final String messageKey;
    private final HttpStatus status;

    public IsActiveAccountException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
        this.status = HttpStatus.FORBIDDEN;
    }
}
