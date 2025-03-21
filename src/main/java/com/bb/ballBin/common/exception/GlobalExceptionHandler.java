package com.bb.ballBin.common.exception;

import com.bb.ballBin.common.exception.model.ErrorResponse;
import com.bb.ballBin.user.service.UserService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final MessageSource messageSource;

    // ✅ 404 Not Found - 요청한 데이터를 찾을 수 없는 경우
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        logger.error(ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus(), messageSource.getMessage(ex.getMessageKey(), null, LocaleContextHolder.getLocale())));
    }

    // ✅ 400 Bad Request - 잘못된 비밀번호 입력 시 발생
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordException(InvalidPasswordException ex) {
        logger.error(ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus(), messageSource.getMessage(ex.getMessageKey(), null, LocaleContextHolder.getLocale())));
    }

    // ✅ 400 Bad Request - 변경할 수 없는 필드를 수정하려고 할 때 발생
    @ExceptionHandler(ImmutableFieldException.class)
    public ResponseEntity<ErrorResponse> handleImmutableFieldException(ImmutableFieldException ex) {
        logger.error(ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus(), messageSource.getMessage(ex.getMessageKey(), null, LocaleContextHolder.getLocale())));
    }

    // ✅ 400 Bad Request - 일반적인 런타임 예외 처리
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        logger.error(ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, messageSource.getMessage(ex.getMessage(), null, LocaleContextHolder.getLocale())));
    }

    // ✅ 400 Bad Request - 잘못된 필드 값 오류 
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.error(ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, messageSource.getMessage(ex.getMessage(), null, LocaleContextHolder.getLocale())));
    }

    // ✅ 400 Bad Request - 입력값 검증 오류
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ConstraintViolationException ex) {
        logger.error(ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, messageSource.getMessage(ex.getMessage(), null, LocaleContextHolder.getLocale())));
    }

    // ✅ 403 Forbidden - 권한 부족
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(AccessDeniedException ex) {
        logger.error(ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN, messageSource.getMessage(ex.getMessage(), null, LocaleContextHolder.getLocale())));
    }

    // ✅ 413 Payload Too Large - 파일 크기 초과
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileSizeException(MaxUploadSizeExceededException ex) {
        logger.error(ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(HttpStatus.PAYLOAD_TOO_LARGE, messageSource.getMessage(ex.getMessage(), null, LocaleContextHolder.getLocale())));
    }

    // ✅ 500 Internal Server Error - 서버 내부 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        String errorMessage;
        try {
            errorMessage = messageSource.getMessage(ex.getMessage(), null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            errorMessage = ex.getMessage();
        }

        logger.error(ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage));
    }
}
