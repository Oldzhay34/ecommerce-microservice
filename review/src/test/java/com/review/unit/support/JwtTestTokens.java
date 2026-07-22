package com.review.unit.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * review servisine özel JWT üretici. Token'lar uygulamanın gerçekten kullandığı
 * HS256 + paylaşılan secret ile imzalanır; böylece testler JwtTokenProvider'ı
 * mock'lamadan gerçek doğrulama yolundan geçer.
 */
public final class JwtTestTokens {

    /** application.yaml'daki jwt.secret varsayılanıyla aynı. */
    public static final String SECRET = "EcommerceSharedJwtSecretKey2026VeryLongAndSecure32Bytes";

    public static final String OTHER_SECRET = "CompletelyDifferentSecretKeyForNegativeTests2026XXXXXX";

    private JwtTestTokens() {
    }

    public static String token(String userId, String rolesClaimValue) {
        return signedWith(SECRET, userId, "roles", rolesClaimValue, 3_600_000L);
    }

    public static String tokenWithClaimName(String userId, String claimName, String rolesClaimValue) {
        return signedWith(SECRET, userId, claimName, rolesClaimValue, 3_600_000L);
    }

    public static String tokenWithoutRoles(String userId) {
        return signedWith(SECRET, userId, null, null, 3_600_000L);
    }

    public static String expiredToken(String userId, String rolesClaimValue) {
        return signedWith(SECRET, userId, "roles", rolesClaimValue, -3_600_000L);
    }

    public static String tokenSignedWithWrongSecret(String userId, String rolesClaimValue) {
        return signedWith(OTHER_SECRET, userId, "roles", rolesClaimValue, 3_600_000L);
    }

    public static String customerToken(String userId) {
        return token(userId, "CUSTOMER");
    }

    public static String storeToken(String userId) {
        return token(userId, "STORE");
    }

    public static String adminToken(String userId) {
        return token(userId, "ADMIN");
    }

    private static String signedWith(String secret, String subject, String claimName,
                                     String claimValue, long ttlMillis) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(now - 60_000L))
                .expiration(new Date(now + ttlMillis));

        if (claimName != null) {
            builder = builder.claim(claimName, claimValue);
        }
        return builder.signWith(key).compact();
    }
}
