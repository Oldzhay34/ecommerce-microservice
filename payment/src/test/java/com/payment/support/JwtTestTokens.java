package com.payment.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * payment servisine ÖZEL JWT üretici (başka bir servisle paylaşılan test modülü yoktur).
 * <p>
 * Token'lar uygulamanın gerçekten doğruladığı algoritmayla (HS256 + paylaşılan secret)
 * imzalanır; böylece testler {@code JwtTokenProvider}'ı mock'lamadan gerçek doğrulama
 * yolundan geçer.
 */
public final class JwtTestTokens {

    /** application.yaml'daki jwt.secret varsayılanıyla birebir aynı. */
    public static final String SECRET = "EcommerceSharedJwtSecretKey2026VeryLongAndSecure32Bytes";

    /** Negatif testler için: imza doğrulamasının gerçekten yapıldığını kanıtlar. */
    public static final String OTHER_SECRET = "CompletelyDifferentPaymentSecretForNegativeTests2026XX";

    private JwtTestTokens() {
    }

    public static String token(String subject, String rolesClaimValue) {
        return signedWith(SECRET, subject, "roles", rolesClaimValue, 3_600_000L);
    }

    public static String tokenWithClaimName(String subject, String claimName, String rolesClaimValue) {
        return signedWith(SECRET, subject, claimName, rolesClaimValue, 3_600_000L);
    }

    public static String tokenWithoutRoles(String subject) {
        return signedWith(SECRET, subject, null, null, 3_600_000L);
    }

    public static String expiredToken(String subject, String rolesClaimValue) {
        return signedWith(SECRET, subject, "roles", rolesClaimValue, -3_600_000L);
    }

    public static String tokenSignedWithWrongSecret(String subject, String rolesClaimValue) {
        return signedWith(OTHER_SECRET, subject, "roles", rolesClaimValue, 3_600_000L);
    }

    public static String customerToken(UUID customerId) {
        return token(customerId.toString(), "CUSTOMER");
    }

    public static String storeToken(UUID storeId) {
        return token(storeId.toString(), "STORE");
    }

    public static String adminToken(UUID adminId) {
        return token(adminId.toString(), "ADMIN");
    }

    public static String bearer(String token) {
        return "Bearer " + token;
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
