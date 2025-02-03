package com.bb.ballBin.common.aop.aspect;

import com.bb.ballBin.common.message.annotation.MessageKey;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Aspect
@Component
public class MessageAspect {

    private final MessageSource messageSource;

    public MessageAspect(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Controller Aspect
     */
    @Around("@annotation(messageKey)")
    public Object handleMessage(ProceedingJoinPoint joinPoint, MessageKey messageKey) throws Throwable {

        String key = messageKey.value();
        Object proceed = joinPoint.proceed();
        String message = messageSource.getMessage(key, null, Locale.KOREAN);

        if (proceed instanceof ResponseEntity<?> response) {
            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(message);
        }

        return proceed;
    }

    /**
     * Service Aspect
     */
    @AfterThrowing(pointcut = "@within(org.springframework.stereotype.Service)", throwing = "exception")
    public void handleException(Throwable exception) {

        String messageKey = exception.getMessage();
        String message = messageSource.getMessage(messageKey, null, Locale.KOREAN);

        throw new RuntimeException(message, exception);
    }
}
