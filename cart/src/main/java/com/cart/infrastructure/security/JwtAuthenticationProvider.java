package com.cart.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationProvider {

    private final String secretKey;

    // Default değeri Gateway'deki secret ile eşitledik
    public JwtAuthenticationProvider(@Value("${jwt.secret:superSecretKeyThatIsVeryLongAndSecureForJwtTokenValidation2026}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);

        // 1. İhtimal: Auth servis "role" olarak (String) dönmüşse (Sizin senaryonuz)
        Object role = claims.get("role");
        if (role != null) {
            return Collections.singletonList(role.toString());
        }

        // 2. İhtimal: Auth servis "roles" olarak (List) dönmüşse
        Object roles = claims.get("roles");
        if (roles instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> roleList = (List<String>) roles;
            return roleList;
        }

        // Hiçbiri yoksa boş liste dön ki NullPointerException atmasın
        return Collections.emptyList();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            // İmza yanlışsa, süresi geçmişse veya token bozuksa buraya düşer
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}