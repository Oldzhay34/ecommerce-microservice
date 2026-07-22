package com.payment.unit.security;

import com.payment.infrastructure.security.JwtAuthFilter;
import com.payment.infrastructure.security.JwtTokenProvider;
import com.payment.support.JwtTestTokens;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Katman: UNIT - JWT filtresi. Filtre kimlik doğrulaması BAŞARISIZ olsa bile
 * zinciri kesmez; reddetme işini Spring Security'nin yetkilendirme katmanı yapar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - JwtAuthFilter (SecurityContext doldurma)")
class JwtAuthFilterTest {

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", JwtTestTokens.SECRET);
        filter = new JwtAuthFilter(provider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("U85: doFilterInternal - Geçerli Bearer token SecurityContext'i doldurur")
    void doFilterInternal_WhenBearerTokenIsValid_ShouldPopulateSecurityContext() throws Exception {
        UUID customerId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", JwtTestTokens.bearer(JwtTestTokens.customerToken(customerId)));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo(customerId.toString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("U86: doFilterInternal - Authorization başlığı yoksa context boş kalır, zincir devam eder")
    void doFilterInternal_WhenNoAuthorizationHeader_ShouldLeaveContextEmptyAndContinueChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("U87: doFilterInternal - Bearer öneki olmayan başlık yok sayılır")
    void doFilterInternal_WhenHeaderIsNotBearer_ShouldIgnoreIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("U88: doFilterInternal - Yanlış secret ile imzalanmış token context'i DOLDURMAZ")
    void doFilterInternal_WhenTokenSignatureIsInvalid_ShouldNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", JwtTestTokens.bearer(
                JwtTestTokens.tokenSignedWithWrongSecret(UUID.randomUUID().toString(), "ADMIN")));

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("U89: doFilterInternal - Süresi dolmuş token context'i doldurmaz ama zinciri de kesmez")
    void doFilterInternal_WhenTokenIsExpired_ShouldNotAuthenticateButContinueChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", JwtTestTokens.bearer(
                JwtTestTokens.expiredToken(UUID.randomUUID().toString(), "CUSTOMER")));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
