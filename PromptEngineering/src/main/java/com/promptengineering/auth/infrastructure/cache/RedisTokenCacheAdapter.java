package com.promptengineering.auth.infrastructure.cache;

import com.promptengineering.auth.application.port.out.TokenCachePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class RedisTokenCacheAdapter implements TokenCachePort {

    private final StringRedisTemplate redisTemplate;

    public RedisTokenCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveOtp(String email, String plainTextOtp, long expirationSeconds) {
        redisTemplate.opsForValue().set("otp:auth:" + email, plainTextOtp, expirationSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<String> getOtp(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get("otp:auth:" + email));
    }

    @Override
    public void deleteOtp(String email) {
        redisTemplate.delete("otp:auth:" + email);
    }

    @Override
    public void blacklistToken(String jti, long ttlSeconds) {
        redisTemplate.opsForValue().set("jwt:blacklist:" + jti, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + jti));
    }
}