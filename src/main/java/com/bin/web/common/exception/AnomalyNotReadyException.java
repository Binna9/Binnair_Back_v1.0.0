package com.bin.web.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AnomalyNotReadyException extends RuntimeException {

    private final String messageKey;
    private final HttpStatus status;

    public AnomalyNotReadyException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
        this.status = HttpStatus.SERVICE_UNAVAILABLE;
    }
}
