package com.campusdoc.user.service;

import com.campusdoc.config.JwtProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AuthTokenService {

    private static final String KEY_PREFIX = "campus:auth:token:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public AuthTokenService(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
    }

    public void storeActiveToken(Long userId, String token) {
        String key = KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, token, jwtProperties.getExpireSeconds(), TimeUnit.SECONDS);
    }

    public void removeToken(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    public boolean isTokenActive(Long userId, String token) {
        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return token != null && token.equals(stored);
    }

    public String getStoredToken(Long userId) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + userId);
    }

    public String redisKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
