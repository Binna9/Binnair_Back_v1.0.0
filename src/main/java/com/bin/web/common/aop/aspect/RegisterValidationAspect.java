package com.bin.web.common.aop.aspect;

import com.bin.web.common.exception.InvalidPasswordException;
import com.bin.web.register.model.RegisterRequestDto;
import com.bin.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RegisterValidationAspect {

    private final UserRepository userRepository;

    @Around("@annotation(com.bin.web.common.annotation.CheckUserRegisterValid)")
    public Object validateRegister(ProceedingJoinPoint joinPoint) throws Throwable {

        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof RegisterRequestDto dto) {

                // 비밀번호 일치
                if (!dto.getLoginPassword().equals(dto.getConfirmPassword())) {
                    throw new InvalidPasswordException("error.password.mismatch");
                }
                // 비밀번호 형식
                String password = dto.getLoginPassword();

                if (password.length() < 8) {
                    throw new InvalidPasswordException("error.password1");
                }
                if (!password.matches(".*[0-9].*")) {
                    throw new InvalidPasswordException("error.password2");
                }
                if (!password.matches(".*[a-zA-Z].*")) {
                    throw new InvalidPasswordException("error.password3");
                }
                if (!password.matches(".*[!@#$%^&*()_+=-].*")) {
                    throw new InvalidPasswordException("error.password4");
                }

                // 필드 중복 검사
                if (userRepository.existsByLoginId(dto.getLoginId())) {
                    throw new InvalidPasswordException("error.loginId.duplicate");
                }
                if (userRepository.existsByEmail(dto.getEmail())) {
                    throw new InvalidPasswordException("error.email.duplicate");
                }
                if (userRepository.existsByNickName(dto.getNickName())) {
                    throw new InvalidPasswordException("error.nickname.duplicate");
                }

                break;
            }
        }

        return joinPoint.proceed();
    }
}
