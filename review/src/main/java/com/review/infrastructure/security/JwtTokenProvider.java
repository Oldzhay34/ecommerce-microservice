package com.review.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final Key key;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith((SecretKey) key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser().verifyWith((SecretKey) key).build()
                .parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser().verifyWith((SecretKey) key).build()
                .parseSignedClaims(token).getPayload();

        String rolesStr = claims.get("roles", String.class);
        if (rolesStr == null) rolesStr = claims.get("role", String.class);
        if (rolesStr == null) rolesStr = claims.get("authorities", String.class);

        if (rolesStr == null || rolesStr.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rolesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}