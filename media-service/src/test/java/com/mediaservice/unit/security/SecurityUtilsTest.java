package com.mediaservice.unit.security;

import com.mediaservice.domain.exception.UnauthorizedMediaAccessException;
import com.mediaservice.infrastructure.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Katman: UNIT - JWT'den kimlik cikarma yardimcisi.
 */
@DisplayName("UNIT - SecurityUtils")
class SecurityUtilsTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("U1: extractUserIdFromSecurityContext - Authentication yoksa UnauthorizedMediaAccessException")
    void extractUserId_WhenNoAuthentication_ShouldThrow() {
        assertThatThrownBy(SecurityUtils::extractUserIdFromSecurityContext)
                .isInstanceOf(UnauthorizedMediaAccessException.class);
    }

    @Test
    @DisplayName("U2: extractUserIdFromSecurityContext - UUID principal ise dogrudan doner")
    void extractUserId_WhenUuidPrincipal_ShouldReturnIt() {
        UUID userId = UUID.randomUUID();
        authenticateAs(userId, "ROLE_STORE");

        assertThat(SecurityUtils.extractUserIdFromSecurityContext()).isEqualTo(userId);
    }

    @Test
    @DisplayName("U3: extractUserIdFromSecurityContext - String principal UUID'ye cevrilebiliyorsa fallback calisir")
    void extractUserId_WhenStringPrincipalParsesAsUuid_ShouldFallback() {
        UUID userId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(userId.toString(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STORE")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityUtils.extractUserIdFromSecurityContext()).isEqualTo(userId);
    }

    @Test
    @DisplayName("U4: extractUserIdFromSecurityContext - Principal UUID'ye cevrilemiyorsa exception firlatir")
    void extractUserId_WhenPrincipalNotParseable_ShouldThrow() {
        var auth = new UsernamePasswordAuthenticationToken("not-a-uuid", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STORE")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(SecurityUtils::extractUserIdFromSecurityContext)
                .isInstanceOf(UnauthorizedMediaAccessException.class);
    }

    @Test
    @DisplayName("U5: isAdmin/hasRole - Yalnizca sahip olunan rol icin true doner")
    void isAdminAndHasRole_ShouldMatchGrantedAuthorities() {
        authenticateAs(UUID.randomUUID(), "ROLE_ADMIN");

        assertThat(SecurityUtils.isAdmin()).isTrue();
        assertThat(SecurityUtils.hasRole("ROLE_ADMIN")).isTrue();
        assertThat(SecurityUtils.hasRole("ROLE_STORE")).isFalse();
    }

    @Test
    @DisplayName("U6: hasRole - Authentication yoksa false doner (exception firlatmaz)")
    void hasRole_WhenNoAuthentication_ShouldReturnFalse() {
        assertThat(SecurityUtils.hasRole(SecurityUtils.ROLE_ADMIN)).isFalse();
    }

    private void authenticateAs(UUID userId, String role) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null,
                Collections.singletonList(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
