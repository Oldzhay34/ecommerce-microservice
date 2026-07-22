package com.review.unit.security;

import com.review.infrastructure.security.JwtTokenProvider;
import com.review.unit.support.JwtTestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Review Service - Unit: JwtTokenProvider")
class JwtTokenProviderTest {

    private final JwtTokenProvider provider = new JwtTokenProvider(JwtTestTokens.SECRET);

    @Test
    @DisplayName("U70: validateToken - Doğru secret ile imzalanmış geçerli token kabul edilir")
    void validateToken_WhenTokenIsProperlySigned_ShouldReturnTrue() {
        assertThat(provider.validateToken(JwtTestTokens.customerToken("cust-1"))).isTrue();
    }

    @Test
    @DisplayName("U71: validateToken - Farklı secret ile imzalanmış token reddedilir")
    void validateToken_WhenSignedWithWrongSecret_ShouldReturnFalse() {
        assertThat(provider.validateToken(
                JwtTestTokens.tokenSignedWithWrongSecret("cust-1", "CUSTOMER"))).isFalse();
    }

    @Test
    @DisplayName("U72: validateToken - Süresi dolmuş token reddedilir")
    void validateToken_WhenTokenIsExpired_ShouldReturnFalse() {
        assertThat(provider.validateToken(JwtTestTokens.expiredToken("cust-1", "CUSTOMER"))).isFalse();
    }

    @ParameterizedTest(name = "U73-{index}: \"{0}\" reddedilir")
    @ValueSource(strings = {"", "bu.bir.jwt.degil", "abc", "a.b.c"})
    @DisplayName("U73: validateToken - Bozuk/anlamsız token'lar exception sızdırmadan reddedilir")
    void validateToken_WhenTokenIsMalformed_ShouldReturnFalseWithoutThrowing(String token) {
        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("U74: getUserIdFromToken - Token subject'ini (userId) döner")
    void getUserIdFromToken_WhenTokenIsValid_ShouldReturnSubject() {
        assertThat(provider.getUserIdFromToken(JwtTestTokens.customerToken("cust-42")))
                .isEqualTo("cust-42");
    }

    @Test
    @DisplayName("U75: getRolesFromToken - roles claim'i virgülle ayrılmış roller olarak parse edilir")
    void getRolesFromToken_WhenRolesClaimPresent_ShouldSplitByComma() {
        String token = JwtTestTokens.token("cust-1", "CUSTOMER,STORE");

        assertThat(provider.getRolesFromToken(token)).containsExactly("CUSTOMER", "STORE");
    }

    @Test
    @DisplayName("U76: getRolesFromToken - roles yoksa role claim'ine düşülür (fallback zinciri 1)")
    void getRolesFromToken_WhenOnlyRoleClaimPresent_ShouldFallBackToRole() {
        String token = JwtTestTokens.tokenWithClaimName("cust-1", "role", "ADMIN");

        assertThat(provider.getRolesFromToken(token)).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("U77: getRolesFromToken - roles/role yoksa authorities claim'ine düşülür (fallback zinciri 2)")
    void getRolesFromToken_WhenOnlyAuthoritiesClaimPresent_ShouldFallBackToAuthorities() {
        String token = JwtTestTokens.tokenWithClaimName("cust-1", "authorities", "ROLE_STORE");

        assertThat(provider.getRolesFromToken(token)).containsExactly("ROLE_STORE");
    }

    @Test
    @DisplayName("U78: getRolesFromToken - Hiçbir rol claim'i yoksa boş liste döner (null dönmez)")
    void getRolesFromToken_WhenNoRoleClaimAtAll_ShouldReturnEmptyList() {
        List<String> roles = provider.getRolesFromToken(JwtTestTokens.tokenWithoutRoles("cust-1"));

        assertThat(roles).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("U79: getRolesFromToken - Boş/whitespace rol claim'i boş liste üretir")
    void getRolesFromToken_WhenRolesClaimIsBlank_ShouldReturnEmptyList() {
        assertThat(provider.getRolesFromToken(JwtTestTokens.token("cust-1", "   "))).isEmpty();
    }

    @Test
    @DisplayName("U80: getRolesFromToken - Roller trim edilir ve boş parçalar atılır")
    void getRolesFromToken_WhenRolesHaveWhitespaceAndEmptyParts_ShouldTrimAndFilter() {
        assertThat(provider.getRolesFromToken(JwtTestTokens.token("cust-1", " CUSTOMER , , ADMIN ")))
                .containsExactly("CUSTOMER", "ADMIN");
    }
}
