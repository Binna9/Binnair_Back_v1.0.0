package com.bb.ballBin.common.aop.aspect;

import com.bb.ballBin.common.message.annotation.MessageKey;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Aspect
@Component
@RequiredArgsConstructor
public class MessageAspect {

    private final MessageSource messageSource;

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
    @AfterThrowing(pointcut = "@within(org.springframework.stereotype.Service) || @within(org.springframework.stereotype.Component)",
            throwing = "exception")
    public void handleException(Throwable exception) {

        // 예외의 메시지(키)를 가져오는데 null이면 기본 키로 대체
        String key = exception.getMessage();
        if (key == null || key.isBlank()) {
            key = "error.unknown";
        }

        String message;
        try {
            message = messageSource.getMessage(key, null, Locale.KOREAN);
        } catch (NoSuchMessageException e) {
            // 만약 해당 키가 없으면 기본 메시지로 대체
            message = messageSource.getMessage("error.unknown", null, Locale.KOREAN);
        }

        throw new RuntimeException(message, exception);
    }
}
