package com.bin.web.common.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class MessageService {

    @Autowired
    private MessageSource messageSource;

    public String getMessage(String key) {
        return messageSource.getMessage(key, null, Locale.KOREAN);
    }

    /**
     * 메시지 가져오기 (파라미터 포함)
     */
    public String getMessage(String key, Object[] args) {
        return messageSource.getMessage(key, args, Locale.KOREAN);
    }

    /**
     * 로케일에 따라 메시지 가져오기
     */
    public String getMessage(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }
}
