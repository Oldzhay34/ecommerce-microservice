package com.promptengineering.auth.application.port.out;

import java.util.Optional;

public interface TokenCachePort {
    void saveOtp(String email, String plainTextOtp, long expirationSeconds);
    Optional<String> getOtp(String email);
    void deleteOtp(String email);
    void blacklistToken(String jti, long ttlSeconds);
    boolean isTokenBlacklisted(String jti);
}