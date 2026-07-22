package com.payment.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    // [NOT: Bu sınıf JWT ÜRETMEZ, yalnızca Auth Service tarafından üretilmiş token'ı doğrular.]

    @Value("${jwt.secret}")
    private String jwtSecret;

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.getSubject();

        // Claim adını auth servisinin ürettiğiyle eşleştir (roles / role / authorities)
        String rolesStr = claims.get("roles", String.class);
        if (rolesStr == null) {
            rolesStr = claims.get("role", String.class);        // alternatif isimler
        }
        if (rolesStr == null) {
            rolesStr = claims.get("authorities", String.class);
        }

        Collection<? extends GrantedAuthority> authorities;
        if (rolesStr == null || rolesStr.isBlank()) {
            authorities = java.util.List.of();                  // NPE yerine boş yetki
        } else {
            authorities = Arrays.stream(rolesStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        return new UsernamePasswordAuthenticationToken(userId, "", authorities);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}