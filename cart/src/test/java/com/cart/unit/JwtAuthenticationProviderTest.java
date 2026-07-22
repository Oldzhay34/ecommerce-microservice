package com.cart.unit;

import com.cart.infrastructure.security.JwtAuthenticationProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: UNIT (altyapı yok, gerçek jjwt kütüphanesiyle token üretiliyor).
 * Hedef: extractRoles'ün üç dallı fallback zinciri ("role" -> "roles" -> boş)
 * ve isTokenValid'in tüm hata yolları.
 */
@DisplayName("UNIT - JwtAuthenticationProvider")
class JwtAuthenticationProviderTest {

    private static final String SECRET = "EcommerceSharedJwtSecretKey2026VeryLongAndSecure32Bytes";
    private static final String OTHER_SECRET = "CompletelyDifferentSecretKeyForNegativeTesting2026Long";

    private final JwtAuthenticationProvider provider = new JwtAuthenticationProvider(SECRET);

    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static String token(String secret, String subject, Map<String, Object> claims, long ttlMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(new Date(now - 1000))
                .expiration(new Date(now + ttlMillis))
                .signWith(key(secret))
                .compact();
    }

    @Test
    @DisplayName("U37: extractUserId - Token'ın subject alanı kullanıcı kimliği olarak döner")
    void extractUserId_WhenTokenValid_ShouldReturnSubject() {
        String jwt = token(SECRET, "user-42", Map.of("role", "ROLE_CUSTOMER"), 60_000);

        assertThat(provider.extractUserId(jwt)).isEqualTo("user-42");
    }

    @Test
    @DisplayName("U38: extractRoles - 'role' (String) claim'i varsa tek elemanlı liste döner")
    void extractRoles_WhenSingleRoleClaimPresent_ShouldReturnSingletonList() {
        String jwt = token(SECRET, "user-1", Map.of("role", "ROLE_ADMIN"), 60_000);

        assertThat(provider.extractRoles(jwt)).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("U39: extractRoles - 'roles' (List) claim'i varsa listenin tamamı döner")
    void extractRoles_WhenRolesListClaimPresent_ShouldReturnWholeList() {
        String jwt = token(SECRET, "user-1", Map.of("roles", List.of("ROLE_CUSTOMER", "ROLE_ADMIN")), 60_000);

        assertThat(provider.extractRoles(jwt)).containsExactly("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("U40: extractRoles - 'role' claim'i 'roles'a göre önceliklidir")
    void extractRoles_WhenBothClaimsPresent_ShouldPreferSingleRoleClaim() {
        String jwt = token(SECRET, "user-1",
                Map.of("role", "ROLE_ADMIN", "roles", List.of("ROLE_CUSTOMER")), 60_000);

        assertThat(provider.extractRoles(jwt)).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("U41: extractRoles - Hiç rol claim'i yoksa boş liste döner (NPE atmaz)")
    void extractRoles_WhenNoRoleClaims_ShouldReturnEmptyList() {
        String jwt = token(SECRET, "user-1", Map.of("foo", "bar"), 60_000);

        assertThat(provider.extractRoles(jwt)).isEmpty();
    }

    @Test
    @DisplayName("U42: isTokenValid - Doğru imzalı ve süresi geçmemiş token için true döner")
    void isTokenValid_WhenTokenWellFormedAndFresh_ShouldReturnTrue() {
        assertThat(provider.isTokenValid(token(SECRET, "user-1", Map.of("role", "ROLE_CUSTOMER"), 60_000))).isTrue();
    }

    @Test
    @DisplayName("U43: isTokenValid - Başka anahtarla imzalanmış token için false döner")
    void isTokenValid_WhenSignedWithDifferentSecret_ShouldReturnFalse() {
        assertThat(provider.isTokenValid(token(OTHER_SECRET, "user-1", Map.of("role", "ROLE_CUSTOMER"), 60_000))).isFalse();
    }

    @Test
    @DisplayName("U44: isTokenValid - Süresi dolmuş token için false döner")
    void isTokenValid_WhenTokenExpired_ShouldReturnFalse() {
        assertThat(provider.isTokenValid(token(SECRET, "user-1", Map.of("role", "ROLE_CUSTOMER"), -1_000))).isFalse();
    }

    @Test
    @DisplayName("U45: isTokenValid - Tamamen bozuk bir string için false döner")
    void isTokenValid_WhenTokenIsGarbage_ShouldReturnFalse() {
        assertThat(provider.isTokenValid("this.is.not.a.jwt")).isFalse();
        assertThat(provider.isTokenValid("")).isFalse();
    }
}
