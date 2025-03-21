package com.bb.ballBin.common.aop.aspect;

import com.bb.ballBin.common.exception.model.ErrorResponse;
import com.bb.ballBin.common.message.annotation.MessageKey;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
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
            HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());

            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }

            ErrorResponse errorResponse = ErrorResponse.of(status, message);

            return ResponseEntity
                    .status(status)
                    .body(errorResponse);
        }

        return proceed;
    }

    /**
     * Service Aspect
     */
    @AfterThrowing(pointcut = "@within(org.springframework.stereotype.Service) || @within(org.springframework.stereotype.Component)",
            throwing = "exception")
    public void handleException(Throwable exception) {

        // ✅ 만약 `RuntimeException`이면 그대로 던지기 (감싸지 않음)
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }

        // ✅ 체크 예외(`Exception`, `Throwable`)인 경우, `RuntimeException`으로 감싸기
        String key = "error.unknown";
        String message;
        try {
            message = messageSource.getMessage(key, null, Locale.KOREAN);
        } catch (NoSuchMessageException e) {
            message = "알 수 없는 오류가 발생했습니다."; // 기본 메시지 설정
        }

        throw new RuntimeException(message, exception);
    }

}
