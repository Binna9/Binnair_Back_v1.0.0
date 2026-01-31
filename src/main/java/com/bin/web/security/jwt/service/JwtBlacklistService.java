package com.bin.web.security.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    public void addToBlacklist(String token, Date expirationTime) {

        long expiration = expirationTime.getTime() - System.currentTimeMillis();

        if (expiration > 0) {
            redisTemplate.opsForValue().set("blacklist:" + token, "blacklisted", expiration, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String token) {

        if (redisTemplate == null) {
            return false;
        }

        Boolean isBlacklisted = redisTemplate.hasKey("blacklist:" + token);

        return Boolean.TRUE.equals(isBlacklisted);
    }
}