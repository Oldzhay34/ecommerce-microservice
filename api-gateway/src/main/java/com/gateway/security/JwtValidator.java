package com.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT dogrulama ve claim cozumleme.
 *
 * ONEMLI: Bu secret, auth-service'in token'i imzaladigi secret ile BIREBIR AYNI
 * olmak ZORUNDADIR. Eslesmezse parseSignedClaims imza dogrulamasi basarisiz olur
 * ve tum token'lar reddedilir.
 *
 * Reactive akista cagrilir ancak jjwt parse islemi CPU-bound ve non-blocking'dir
 * (I/O yapmaz), bu yuzden filter icinde dogrudan cagrilmasi guvenlidir.
 */
@Component
public class JwtValidator {

    private final SecretKey signingKey;

    public JwtValidator(@Value("${app.security.jwt-secret}") String jwtSecret) {
        // HMAC-SHA256 icin secret en az 32 byte olmalidir.
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Token'i dogrular ve claim'leri doner. Gecersiz/suresi dolmus token JwtException firlatir.
     */
    public Claims validateAndGetClaims(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    /**
     * Token gecerli mi? (exception firlatmadan boolean doner)
     */
    public boolean isValid(String token) {
        try {
            validateAndGetClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * JWT subject (userId). Downstream servisler bunu kendi tiplerine (Long/UUID/String) cevirir.
     */
    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    /**
     * Rol claim'ini cozumler. Once "role", yoksa "roles"/"authorities" denenir.
     * Downstream servisler zaten kendi JWT'sini cozdugu icin bu deger sadece
     * X-User-Role header'i ve loglama amaclidir.
     */
    public String extractRole(Claims claims) {
        Object role = claims.get("role");
        if (role != null) {
            return role.toString();
        }
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list && !list.isEmpty()) {
            return list.get(0).toString();
        }
        Object authorities = claims.get("authorities");
        if (authorities instanceof List<?> list) {
            // BUGFIX: dogrudan toString() cagirmak "[ROLE_STORE]" gibi koseli parantezli
            // bir deger uretiyordu ve bu deger X-User-Role header'i olarak downstream'e
            // gidiyordu. "roles" claim'i ile ayni sekilde ilk eleman alinir.
            return list.isEmpty() ? "" : list.get(0).toString();
        }
        if (authorities != null) {
            return authorities.toString();
        }
        return "";
    }
}
