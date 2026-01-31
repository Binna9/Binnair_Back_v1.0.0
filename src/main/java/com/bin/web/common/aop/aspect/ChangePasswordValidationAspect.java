package com.bin.web.common.aop.aspect;

import com.bin.web.common.exception.InvalidPasswordException;
import com.bin.web.user.model.UserPasswordChangeRequestDto;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ChangePasswordValidationAspect {
    @Around("@annotation(com.bin.web.common.annotation.CheckUserChangePasswordValid)")
    public Object validatePasswordChange(ProceedingJoinPoint joinPoint) throws Throwable {

        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof UserPasswordChangeRequestDto dto) {

                String currentPw = dto.getCurrentPassword();
                String newPw = dto.getNewPassword();
                String confirmPw = dto.getConfirmPassword();

                if (currentPw == null || currentPw.isBlank()) {
                    throw new InvalidPasswordException("error.password.current.required");
                }
                if (newPw == null || confirmPw == null) {
                    throw new InvalidPasswordException("error.password.required");
                }

                if (currentPw.equals(newPw)) {
                    throw new InvalidPasswordException("error.password.same.as.current");
                }

                // 비밀번호 일치
                if (!newPw.equals(confirmPw)) {
                    throw new InvalidPasswordException("error.password.mismatch");
                }

                // 비밀번호 정책 (회원가입과 동일)
                if (newPw.length() < 8) {
                    throw new InvalidPasswordException("error.password1");
                }
                if (!newPw.matches(".*[0-9].*")) {
                    throw new InvalidPasswordException("error.password2");
                }
                if (!newPw.matches(".*[a-zA-Z].*")) {
                    throw new InvalidPasswordException("error.password3");
                }
                if (!newPw.matches(".*[!@#$%^&*()_+=-].*")) {
                    throw new InvalidPasswordException("error.password4");
                }

                break;
            }
        }

        return joinPoint.proceed();
    }
}
