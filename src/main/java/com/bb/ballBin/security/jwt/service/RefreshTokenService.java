package com.bb.ballBin.security.jwt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate; // Redis 활용 (DB 가능)

    public void storeRefreshToken(String userId, String refreshToken) {
        // ✅ Redis 에 저장 (7일 동안 유지)
        redisTemplate.opsForValue().set("refreshToken:" + userId, refreshToken, 7, TimeUnit.DAYS);
    }

    public String getRefreshToken(String userId) {
        return redisTemplate.opsForValue().get("refreshToken:" + userId);
    }

    public void deleteRefreshToken(String userId) {
        redisTemplate.delete("refreshToken:" + userId);
    }
}

