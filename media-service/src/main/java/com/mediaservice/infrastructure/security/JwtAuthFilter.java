package com.mediaservice.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

/**
 * Product servisinin JwtResourceFilter'i ile BIREBIR AYNI desen.
 *
 * Kritik uyumluluk noktalari (Product'tan dogrulandi):
 *  1. Secret DUZ STRING olarak UTF-8 byte'a cevrilir - Base64 decode EDILMEZ.
 *     ("EcommerceSharedJwtSecretKey2026VeryLongAndSecure32Bytes", 56 byte)
 *  2. Rol claim'i 'role' - TEKIL STRING. ('roles' listesi DEGIL.)
 *     Yanlis claim adi okunursa authorities bos kalir ve hasRole('STORE') asla gecmez;
 *     tum korumali endpoint'ler SESSIZCE 403 doner.
 *  3. Principal UUID tipindedir (Product: UUID.fromString(claims.getSubject())).
 *  4. Token gecersizse SecurityContext temizlenir, istek zincire devam eder;
 *     yetki karari SecurityConfig'e birakilir (public endpoint'ler token'siz calisir).
 *
 * Gateway ayrica X-User-Id header'i set eder (JwtAuthenticationGlobalFilter), ancak kimlik
 * HEADER'DAN OKUNMAZ: header spoof edilebilir ve rol bilgisi header'da tasinmiyor.
 * Media servisi de Product gibi token'i kendi dogrular (defense in depth).
 *
 * [Karar E] JWT 'sub' = userId. Sistemde ayri bir storeId attribute'u YOKTUR;
 * ROLE_STORE tasiyan kullanicinin userId'si magaza kimligidir.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final SecretKey key;

    public JwtAuthFilter(@Value("${app.security.jwt-secret}") String jwtSecret) {
        // Identity servisiyle paylasilan simetrik anahtar (Product ile birebir ayni yontem)
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null) {
                Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt).getPayload();

                UUID userId = UUID.fromString(claims.getSubject());
                String role = claims.get("role", String.class);

                // Spring Security'nin role matcher'i ile calisabilmesi icin "ROLE_" prefix'i
                // ekleniyor (or: ROLE_STORE)
                if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.singletonList(new SimpleGrantedAuthority(role)));

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext(); // Token gecersizse temizle
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}