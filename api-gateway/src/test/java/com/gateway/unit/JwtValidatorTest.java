package com.gateway.unit;

import com.gateway.security.JwtValidator;
import com.gateway.support.JwtTestTokens;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtValidator whitebox unit testleri. Ag/altyapi yok, sadece imza + claim mantigi.
 */
class JwtValidatorTest {

    private JwtValidator jwtValidator;

    @BeforeEach
    void setUp() {
        jwtValidator = new JwtValidator(JwtTestTokens.SECRET);
    }

    @Test
    @DisplayName("U1: validateAndGetClaims - Gecerli imzali token'in claim'lerini doner")
    void validateAndGetClaims_WhenTokenIsValid_ShouldReturnClaims() {
        String token = JwtTestTokens.valid("user-42", "ROLE_STORE");

        Claims claims = jwtValidator.validateAndGetClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user-42");
        assertThat(claims.get("role")).isEqualTo("ROLE_STORE");
        assertThat(claims.getExpiration()).isInTheFuture();
    }

    @Test
    @DisplayName("U2: validateAndGetClaims - Baska bir secret ile imzalanmis token'i reddeder (imza dogrulamasi calisiyor)")
    void validateAndGetClaims_WhenSignedWithDifferentSecret_ShouldThrowSignatureException() {
        String forged = JwtTestTokens.signedWithWrongKey("attacker");

        assertThatThrownBy(() -> jwtValidator.validateAndGetClaims(forged))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("U3: validateAndGetClaims - Imzasi kurcalanmis token'i reddeder")
    void validateAndGetClaims_WhenSignatureIsTampered_ShouldThrowJwtException() {
        String tampered = JwtTestTokens.tamperedSignature("user-1");

        assertThatThrownBy(() -> jwtValidator.validateAndGetClaims(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("U4: validateAndGetClaims - Suresi dolmus token ExpiredJwtException firlatir")
    void validateAndGetClaims_WhenTokenIsExpired_ShouldThrowExpiredJwtException() {
        String expired = JwtTestTokens.expired("user-1");

        assertThatThrownBy(() -> jwtValidator.validateAndGetClaims(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("U5: validateAndGetClaims - Bozuk formatli token MalformedJwtException firlatir")
    void validateAndGetClaims_WhenTokenIsMalformed_ShouldThrowMalformedJwtException() {
        assertThatThrownBy(() -> jwtValidator.validateAndGetClaims(JwtTestTokens.malformed()))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("U6: validateAndGetClaims - Bos/null token IllegalArgumentException firlatir")
    void validateAndGetClaims_WhenTokenIsBlank_ShouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> jwtValidator.validateAndGetClaims(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwtValidator.validateAndGetClaims(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("U7: validateAndGetClaims - Imzasiz (alg=none) token'i kabul etmez")
    void validateAndGetClaims_WhenTokenIsUnsigned_ShouldThrowJwtException() {
        String unsigned = Jwts.builder().subject("attacker").claim("role", "ROLE_ADMIN").compact();

        assertThatThrownBy(() -> jwtValidator.validateAndGetClaims(unsigned))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("U8: isValid - Gecerli token icin true, gecersiz senaryolarin hepsi icin false doner")
    void isValid_WhenTokenIsInvalid_ShouldReturnFalseWithoutThrowing() {
        assertThat(jwtValidator.isValid(JwtTestTokens.valid("u1", "ROLE_USER"))).isTrue();

        assertThat(jwtValidator.isValid(JwtTestTokens.expired("u1"))).isFalse();
        assertThat(jwtValidator.isValid(JwtTestTokens.signedWithWrongKey("u1"))).isFalse();
        assertThat(jwtValidator.isValid(JwtTestTokens.tamperedSignature("u1"))).isFalse();
        assertThat(jwtValidator.isValid(JwtTestTokens.malformed())).isFalse();
        assertThat(jwtValidator.isValid("")).isFalse();
        assertThat(jwtValidator.isValid(null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a.b", "....", "Bearer abc", "eyJhbGciOiJIUzI1NiJ9"})
    @DisplayName("U9: isValid - Cesitli bozuk girdilerde exception sizdirmadan false doner")
    void isValid_WhenInputIsGarbage_ShouldReturnFalse(String garbage) {
        assertThat(jwtValidator.isValid(garbage)).isFalse();
    }

    @Test
    @DisplayName("U10: extractUserId - Subject claim'ini doner, subject yoksa null doner")
    void extractUserId_WhenSubjectMissing_ShouldReturnNull() {
        Claims withSubject = jwtValidator.validateAndGetClaims(JwtTestTokens.valid("user-7", "ROLE_USER"));
        assertThat(jwtValidator.extractUserId(withSubject)).isEqualTo("user-7");

        Claims withoutSubject = jwtValidator.validateAndGetClaims(JwtTestTokens.withoutSubject());
        assertThat(jwtValidator.extractUserId(withoutSubject)).isNull();
    }

    @Test
    @DisplayName("U11: extractRole - 'role' claim'i varsa onu doner")
    void extractRole_WhenRoleClaimPresent_ShouldReturnIt() {
        Claims claims = jwtValidator.validateAndGetClaims(JwtTestTokens.valid("u1", "ROLE_ADMIN"));

        assertThat(jwtValidator.extractRole(claims)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("U12: extractRole - 'role' yoksa 'roles' listesinin ilk elemanini doner")
    void extractRole_WhenOnlyRolesListPresent_ShouldReturnFirstElement() {
        Claims claims = jwtValidator.validateAndGetClaims(
                JwtTestTokens.validWithRolesList("u1", List.of("ROLE_STORE", "ROLE_USER")));

        assertThat(jwtValidator.extractRole(claims)).isEqualTo("ROLE_STORE");
    }

    @Test
    @DisplayName("U13: extractRole - 'roles' bos liste ise 'authorities'e duser, o da yoksa bos string doner")
    void extractRole_WhenRolesListIsEmpty_ShouldFallBackToEmptyString() {
        Claims claims = jwtValidator.validateAndGetClaims(
                JwtTestTokens.validWithRolesList("u1", List.of()));

        assertThat(jwtValidator.extractRole(claims)).isEmpty();
    }

    @Test
    @DisplayName("U14: extractRole - 'authorities' claim'i liste ise ilk elemani duz string olarak doner (koseli parantezsiz)")
    void extractRole_WhenAuthoritiesIsList_ShouldReturnFirstElementWithoutBrackets() {
        Claims claims = jwtValidator.validateAndGetClaims(
                JwtTestTokens.validWithAuthorities("u1", List.of("ROLE_STORE", "ROLE_USER")));

        assertThat(jwtValidator.extractRole(claims)).isEqualTo("ROLE_STORE");
    }

    @Test
    @DisplayName("U15: extractRole - 'authorities' claim'i tek string ise oldugu gibi doner")
    void extractRole_WhenAuthoritiesIsPlainString_ShouldReturnIt() {
        Claims claims = jwtValidator.validateAndGetClaims(
                JwtTestTokens.validWithAuthorities("u1", "ROLE_USER"));

        assertThat(jwtValidator.extractRole(claims)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("U16: extractRole - Hicbir rol claim'i yoksa bos string doner (null degil)")
    void extractRole_WhenNoRoleClaimAtAll_ShouldReturnEmptyString() {
        Claims claims = jwtValidator.validateAndGetClaims(JwtTestTokens.validWithoutRole("u1"));

        assertThat(jwtValidator.extractRole(claims)).isEmpty();
    }

    @Test
    @DisplayName("U17: validateAndGetClaims - Issuer dogrulanmaz; guven siniri paylasilan imza secret'idir")
    void validateAndGetClaims_WhenIssuerDiffers_ShouldStillAcceptBecauseIssuerIsNotChecked() {
        // Bu test mevcut (kasitli) davranisi sabitler: JwtValidator sadece imzayi dogrular.
        // Ayni secret'i bilen her issuer kabul edilir; secret auth-service ile paylasilir.
        // Issuer kontrolu eklenecekse auth-service'in "iss" uretmesi ON KOSULDUR.
        Claims claims = jwtValidator.validateAndGetClaims(
                JwtTestTokens.withIssuer("u1", "some-other-issuer"));

        assertThat(claims.getIssuer()).isEqualTo("some-other-issuer");
    }

    @Test
    @DisplayName("U18: constructor - 32 byte'tan kisa secret ile olusturulamaz (zayif anahtar korumasi)")
    void constructor_WhenSecretIsTooShort_ShouldThrowWeakKeyException() {
        assertThatThrownBy(() -> new JwtValidator("kisa-secret"))
                .isInstanceOf(WeakKeyException.class);
    }
}
