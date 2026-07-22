package com.review.unit.security;

import com.review.infrastructure.security.JwtAuthFilter;
import com.review.infrastructure.security.JwtTokenProvider;
import com.review.unit.support.JwtTestTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtAuthFilter'ın SecurityContext doldurma mantığının whitebox testi.
 * Spring context ayağa kalkmaz; sadece Mock servlet nesneleri kullanılır.
 */
@DisplayName("Review Service - Unit: JwtAuthFilter")
class JwtAuthFilterTest {

    private final JwtAuthFilter filter = new JwtAuthFilter(new JwtTokenProvider(JwtTestTokens.SECRET));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockFilterChain invoke(String method, String uri, String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    private Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    @DisplayName("U81: doFilterInternal - Geçerli CUSTOMER token'ı ROLE_CUSTOMER yetkisiyle kimlik kurar")
    void doFilterInternal_WithValidCustomerToken_ShouldAuthenticateWithRolePrefix() throws Exception {
        invoke("POST", "/api/reviews", "Bearer " + JwtTestTokens.customerToken("cust-1"));

        assertThat(currentAuth()).isNotNull();
        assertThat(currentAuth().getName()).isEqualTo("cust-1");
        assertThat(currentAuth().getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("U82: doFilterInternal - Rol zaten ROLE_ önekliyse ikinci kez öneklenmez")
    void doFilterInternal_WhenRoleAlreadyPrefixed_ShouldNotDoublePrefix() throws Exception {
        invoke("GET", "/api/reviews/all", "Bearer " + JwtTestTokens.token("admin-1", "ROLE_ADMIN"));

        assertThat(currentAuth().getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("U83: doFilterInternal - Birden fazla rol ayrı ayrı yetkiye çevrilir")
    void doFilterInternal_WithMultipleRoles_ShouldMapEachToAuthority() throws Exception {
        invoke("GET", "/api/reviews/me", "Bearer " + JwtTestTokens.token("u-1", "CUSTOMER,ROLE_STORE"));

        assertThat(currentAuth().getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_STORE");
    }

    @Test
    @DisplayName("U84: doFilterInternal - Ürün yorumları herkese açık yoldur, token olmadan da zincir devam eder")
    void doFilterInternal_ForPublicProductPath_ShouldSkipAuthenticationAndContinueChain() throws Exception {
        MockFilterChain chain = invoke("GET", "/api/reviews/product/prod-1", null);

        assertThat(currentAuth()).isNull();
        assertThat(chain.getRequest()).as("zincir devam etmeli").isNotNull();
    }

    @Test
    @DisplayName("U85: doFilterInternal - Public yolda geçerli token gelse bile kimlik kurulmaz (erken çıkış)")
    void doFilterInternal_ForPublicProductPathWithToken_ShouldStillNotAuthenticate() throws Exception {
        invoke("GET", "/api/reviews/product/prod-1", "Bearer " + JwtTestTokens.customerToken("cust-1"));

        assertThat(currentAuth()).isNull();
    }

    @Test
    @DisplayName("U86: doFilterInternal - Authorization header yoksa kimlik kurulmaz ama zincir devam eder")
    void doFilterInternal_WithoutAuthorizationHeader_ShouldNotAuthenticateButContinue() throws Exception {
        MockFilterChain chain = invoke("POST", "/api/reviews", null);

        assertThat(currentAuth()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("U87: doFilterInternal - Bearer olmayan şema (Basic) yok sayılır")
    void doFilterInternal_WithNonBearerScheme_ShouldNotAuthenticate() throws Exception {
        invoke("POST", "/api/reviews", "Basic dXNlcjpwYXNz");

        assertThat(currentAuth()).isNull();
    }

    @Test
    @DisplayName("U88: doFilterInternal - Yanlış secret ile imzalanmış token kimlik kurmaz")
    void doFilterInternal_WithTokenSignedByWrongSecret_ShouldNotAuthenticate() throws Exception {
        invoke("POST", "/api/reviews",
                "Bearer " + JwtTestTokens.tokenSignedWithWrongSecret("cust-1", "CUSTOMER"));

        assertThat(currentAuth()).isNull();
    }

    @Test
    @DisplayName("U89: doFilterInternal - Süresi dolmuş token kimlik kurmaz")
    void doFilterInternal_WithExpiredToken_ShouldNotAuthenticate() throws Exception {
        invoke("POST", "/api/reviews", "Bearer " + JwtTestTokens.expiredToken("cust-1", "CUSTOMER"));

        assertThat(currentAuth()).isNull();
    }

    @Test
    @DisplayName("U90: doFilterInternal - Rolsüz geçerli token kimlik kurar ama hiç yetki taşımaz (403 yolu)")
    void doFilterInternal_WithValidTokenWithoutRoles_ShouldAuthenticateWithNoAuthorities() throws Exception {
        invoke("POST", "/api/reviews", "Bearer " + JwtTestTokens.tokenWithoutRoles("cust-1"));

        assertThat(currentAuth()).isNotNull();
        assertThat(currentAuth().getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("U91: doFilterInternal - /api/reviews/product öneki tam eşleşme ister, /api/reviews/products korumalıdır")
    void doFilterInternal_WhenPathOnlyLooksLikePublicPath_ShouldNotSkipAuthentication() throws Exception {
        invoke("GET", "/api/reviews/products-summary", "Bearer " + JwtTestTokens.adminToken("admin-1"));

        assertThat(currentAuth()).as("public kısayola düşmemeli, token işlenmeli").isNotNull();
    }
}
