package com.mediaservice.unit.security;

import com.mediaservice.infrastructure.security.JwtAuthFilter;
import com.mediaservice.support.JwtTestTokens;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Katman: UNIT - JWT dogrulama filtresi. Gercek imza dogrulamasindan gecer
 * (JwtTestTokens ile ayni secret'la imzalanir); Spring context yoktur.
 */
@DisplayName("UNIT - JwtAuthFilter")
class JwtAuthFilterTest {

    private final JwtAuthFilter filter = new JwtAuthFilter(JwtTestTokens.SECRET);

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("U1: Gecerli token - SecurityContext'e UUID principal ve ROLE_ prefixli authority set edilir")
    void doFilterInternal_WhenValidToken_ShouldSetAuthenticationWithRolePrefix() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", JwtTestTokens.bearer(JwtTestTokens.storeToken(userId)));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userId);
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_STORE");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("U2: role claim'i zaten ROLE_ ile basliyorsa CIFT prefix EKLENMEZ")
    void doFilterInternal_WhenRoleAlreadyPrefixed_ShouldNotDoublePrefix() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization",
                JwtTestTokens.bearer(JwtTestTokens.tokenWithRole(userId, "ROLE_ADMIN")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("U3: Authorization header yoksa istek zincire devam eder, authentication set EDILMEZ")
    void doFilterInternal_WhenNoAuthorizationHeader_ShouldContinueChainWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("U4: Gecersiz imzali token - SecurityContext TEMIZLENIR ve istek yine de zincire devam eder")
    void doFilterInternal_WhenSignatureInvalid_ShouldClearContextButContinueChain() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization",
                JwtTestTokens.bearer(JwtTestTokens.tokenSignedWithWrongSecret(userId, "STORE")));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("U5: Suresi dolmus token - SecurityContext temizlenir, exception FIRLAMAZ")
    void doFilterInternal_WhenTokenExpired_ShouldClearContextWithoutThrowing() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", JwtTestTokens.bearer(JwtTestTokens.expiredStoreToken(userId)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
