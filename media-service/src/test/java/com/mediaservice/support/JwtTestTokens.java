package com.mediaservice.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * media-service'e OZEL JWT uretici. product-service ile BIREBIR ayni sozlesmeyi
 * taklit eder (bkz. JwtAuthFilter): 'sub' = userId (UUID), rol TEKIL 'role' claim'inde
 * ("STORE"/"ADMIN"/"CUSTOMER" - ROLE_ prefixi filter tarafindan eklenir), secret DUZ
 * STRING olarak UTF-8 byte'a cevrilir (Base64 decode EDILMEZ).
 * <p>
 * Token'lar gercekten dogrulanan algoritmayla (HS256 + paylasilan secret) imzalanir;
 * boylece testler JwtAuthFilter'i mock'lamadan gercek dogrulama yolundan gecer.
 */
public final class JwtTestTokens {

    /** application.yaml'daki app.security.jwt-secret varsayiliyla birebir ayni. */
    public static final String SECRET = "EcommerceSharedJwtSecretKey2026VeryLongAndSecure32Bytes";

    /** Negatif testler icin: imza dogrulamasinin gercekten yapildigini kanitlar. */
    public static final String OTHER_SECRET = "CompletelyDifferentMediaSecretForNegativeTests2026XX";

    private JwtTestTokens() {
    }

    public static String storeToken(UUID storeUserId) {
        return signedWith(SECRET, storeUserId.toString(), "STORE", 3_600_000L);
    }

    public static String adminToken(UUID adminUserId) {
        return signedWith(SECRET, adminUserId.toString(), "ADMIN", 3_600_000L);
    }

    public static String customerToken(UUID customerId) {
        return signedWith(SECRET, customerId.toString(), "CUSTOMER", 3_600_000L);
    }

    public static String tokenWithRole(UUID userId, String role) {
        return signedWith(SECRET, userId.toString(), role, 3_600_000L);
    }

    public static String expiredStoreToken(UUID storeUserId) {
        return signedWith(SECRET, storeUserId.toString(), "STORE", -3_600_000L);
    }

    public static String tokenSignedWithWrongSecret(UUID userId, String role) {
        return signedWith(OTHER_SECRET, userId.toString(), role, 3_600_000L);
    }

    public static String tokenWithoutRoleClaim(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date(now - 60_000L))
                .expiration(new Date(now + 3_600_000L))
                .signWith(key)
                .compact();
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String signedWith(String secret, String subject, String role, long ttlMillis) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date(now - 60_000L))
                .expiration(new Date(now + ttlMillis))
                .signWith(key)
                .compact();
    }
}
