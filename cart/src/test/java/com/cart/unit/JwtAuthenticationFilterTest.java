package com.cart.unit;

import com.cart.infrastructure.security.JwtAuthenticationProvider;
import com.cart.infrastructure.security.filter.JwtAuthenticationFilter;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT. Gerçek servlet konteyneri yok; MockHttpServletRequest ile
 * filtre mantığının her guard'ı ayrı ayrı sürülüyor.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtAuthenticationProvider provider;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(provider);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("U46: doFilterInternal - Authorization header yoksa zincir devam eder, context boş kalır")
    void doFilterInternal_WhenNoAuthorizationHeader_ShouldContinueChainWithEmptyContext() throws Exception {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(provider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("U47: doFilterInternal - Header 'Bearer ' ile başlamıyorsa token hiç doğrulanmaz")
    void doFilterInternal_WhenHeaderIsNotBearer_ShouldSkipTokenValidation() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(provider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("U48: doFilterInternal - Token geçersizse context doldurulmaz ama zincir kesilmez")
    void doFilterInternal_WhenTokenInvalid_ShouldLeaveContextEmptyAndContinue() throws Exception {
        request.addHeader("Authorization", "Bearer bad-token");
        when(provider.isTokenValid("bad-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("U49: doFilterInternal - Geçerli token'da principal userId, authority'ler rol claim'inden kurulur")
    void doFilterInternal_WhenTokenValid_ShouldPopulateAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer good-token");
        when(provider.isTokenValid("good-token")).thenReturn(true);
        when(provider.extractUserId("good-token")).thenReturn("11111111-1111-1111-1111-111111111111");
        when(provider.extractRoles("good-token")).thenReturn(List.of("ROLE_CUSTOMER"));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(auth.getAuthorities())
                .extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
        assertThat(auth.getDetails()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("U50: doFilterInternal - Context zaten doluysa mevcut authentication ezilmez")
    void doFilterInternal_WhenContextAlreadyAuthenticated_ShouldNotOverwrite() throws Exception {
        UsernamePasswordAuthenticationToken existing = new UsernamePasswordAuthenticationToken(
                "existing-user", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(existing);

        request.addHeader("Authorization", "Bearer good-token");
        lenient().when(provider.isTokenValid("good-token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("U51: doFilterInternal - Rol claim'i boşsa authority'siz bir authentication kurulur")
    void doFilterInternal_WhenRolesEmpty_ShouldAuthenticateWithoutAuthorities() throws Exception {
        request.addHeader("Authorization", "Bearer good-token");
        when(provider.isTokenValid("good-token")).thenReturn(true);
        when(provider.extractUserId("good-token")).thenReturn("user-1");
        when(provider.extractRoles("good-token")).thenReturn(List.of());

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }
}
