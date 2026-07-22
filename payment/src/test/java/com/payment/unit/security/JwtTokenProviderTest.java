package com.payment.unit.security;

import com.payment.infrastructure.security.JwtTokenProvider;
import com.payment.support.JwtTestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: UNIT - JWT doğrulama. Bu servis token ÜRETMEZ, yalnızca auth servisinin
 * ürettiği token'ı doğrular; testler gerçek imza yolundan geçer (mock yok).
 */
@DisplayName("UNIT - JwtTokenProvider (imza doğrulama ve rol çıkarımı)")
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", JwtTestTokens.SECRET);
    }

    @Test
    @DisplayName("U74: validateToken - Doğru secret ile imzalanmış geçerli token kabul edilir")
    void validateToken_WhenTokenIsProperlySigned_ShouldReturnTrue() {
        assertThat(provider.validateToken(JwtTestTokens.customerToken(UUID.randomUUID()))).isTrue();
    }

    @Test
    @DisplayName("U75: validateToken - Farklı secret ile imzalanmış token reddedilir")
    void validateToken_WhenSignedWithWrongSecret_ShouldReturnFalse() {
        assertThat(provider.validateToken(
                JwtTestTokens.tokenSignedWithWrongSecret(UUID.randomUUID().toString(), "CUSTOMER"))).isFalse();
    }

    @Test
    @DisplayName("U76: validateToken - Süresi dolmuş token reddedilir")
    void validateToken_WhenTokenIsExpired_ShouldReturnFalse() {
        assertThat(provider.validateToken(
                JwtTestTokens.expiredToken(UUID.randomUUID().toString(), "CUSTOMER"))).isFalse();
    }

    @Test
    @DisplayName("U77: validateToken - Bozuk/anlamsız token istisna değil false üretir")
    void validateToken_WhenTokenIsGarbage_ShouldReturnFalseWithoutThrowing() {
        assertThat(provider.validateToken("kesinlikle-jwt-degil")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("U78: getAuthentication - Subject, principal olarak (kullanıcı kimliği) döner")
    void getAuthentication_ShouldExposeSubjectAsPrincipalName() {
        UUID customerId = UUID.randomUUID();

        Authentication authentication = provider.getAuthentication(JwtTestTokens.customerToken(customerId));

        assertThat(authentication.getName()).isEqualTo(customerId.toString());
    }

    @Test
    @DisplayName("U79: getAuthentication - roles claim'i ROLE_ öneki eklenerek yetkiye çevrilir")
    void getAuthentication_WhenRolesClaimIsPresent_ShouldPrefixWithRoleUnderscore() {
        Authentication authentication = provider.getAuthentication(
                JwtTestTokens.token(UUID.randomUUID().toString(), "CUSTOMER"));

        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("U80: getAuthentication - Zaten ROLE_ önekli claim'e ikinci kez önek eklenmez")
    void getAuthentication_WhenClaimAlreadyPrefixed_ShouldNotDoublePrefix() {
        Authentication authentication = provider.getAuthentication(
                JwtTestTokens.token(UUID.randomUUID().toString(), "ROLE_ADMIN"));

        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("U81: getAuthentication - Virgülle ayrılmış çoklu rol ayrıştırılır ve boşluklar kırpılır")
    void getAuthentication_WhenMultipleRoles_ShouldSplitAndTrim() {
        Authentication authentication = provider.getAuthentication(
                JwtTestTokens.token(UUID.randomUUID().toString(), "CUSTOMER, ADMIN ,STORE"));

        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN", "ROLE_STORE");
    }

    @Test
    @DisplayName("U82: getAuthentication - role / authorities alternatif claim adları da desteklenir")
    void getAuthentication_WhenAlternativeClaimNameIsUsed_ShouldStillResolveRoles() {
        assertThat(provider.getAuthentication(
                JwtTestTokens.tokenWithClaimName(UUID.randomUUID().toString(), "role", "ADMIN"))
                .getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");

        assertThat(provider.getAuthentication(
                JwtTestTokens.tokenWithClaimName(UUID.randomUUID().toString(), "authorities", "STORE"))
                .getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_STORE");
    }

    @Test
    @DisplayName("U83: getAuthentication - Rol claim'i hiç yoksa NPE yerine boş yetki listesi döner")
    void getAuthentication_WhenNoRolesClaim_ShouldReturnEmptyAuthoritiesInsteadOfNpe() {
        Authentication authentication = provider.getAuthentication(
                JwtTestTokens.tokenWithoutRoles(UUID.randomUUID().toString()));

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("U84: getAuthentication - Boş rol claim'i boş yetki listesi üretir")
    void getAuthentication_WhenRolesClaimIsBlank_ShouldReturnEmptyAuthorities() {
        Authentication authentication = provider.getAuthentication(
                JwtTestTokens.token(UUID.randomUUID().toString(), "   "));

        assertThat(authentication.getAuthorities()).isEmpty();
    }
}
