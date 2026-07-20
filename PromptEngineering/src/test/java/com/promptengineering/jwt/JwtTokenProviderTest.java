package com.promptengineering.jwt;

import com.promptengineering.auth.domain.model.Customer;
import com.promptengineering.auth.domain.model.User;
import com.promptengineering.auth.infrastructure.security.provider.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    // HMAC-SHA256 için secret en az 256 bit (32 byte) olmalı. Yeterli uzunlukta veriyoruz.
    private static final String TEST_SECRET = "test-secret-key-en-az-256-bit-uzunlugunda-olmali-1234567890";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET);
    }

    private User sampleUser() {
        return new Customer(
                UUID.randomUUID(), "Ali Veli", "ali@example.com", "hashed-pw",
                true, null, null, 0);
    }

    @Test
    @DisplayName("D1: generateToken + validateToken - üretilen token geçerlidir")
    void generateToken_thenValidateToken_shouldReturnTrue() {
        User user = sampleUser();
        String token = jwtTokenProvider.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("D2: generateToken + getEmailFromToken - email claim doğru döner")
    void generateToken_thenGetEmailFromToken_shouldReturnOriginalEmail() {
        User user = sampleUser();
        String token = jwtTokenProvider.generateToken(user);

        String email = jwtTokenProvider.getEmailFromToken(token);
        assertThat(email).isEqualTo("ali@example.com");
    }

    @Test
    @DisplayName("D3: generateToken + getUserIdFromToken - subject id doğru döner")
    void generateToken_thenGetUserIdFromToken_shouldReturnOriginalId() {
        User user = sampleUser();
        String token = jwtTokenProvider.generateToken(user);

        UUID extractedId = jwtTokenProvider.getUserIdFromToken(token);
        assertThat(extractedId).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("D4: validateToken - bozuk token için false döner")
    void validateToken_withMalformedToken_shouldReturnFalse() {
        assertThat(jwtTokenProvider.validateToken("abc.def.ghi")).isFalse();
    }

    @Test
    @DisplayName("D5: validateToken - boş string için false döner")
    void validateToken_withEmptyString_shouldReturnFalse() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }
}