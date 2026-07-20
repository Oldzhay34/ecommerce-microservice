package com.promptengineering.usecase;

import com.promptengineering.auth.api.dto.LoginRequest;
import com.promptengineering.auth.api.dto.RegisterRequest;
import com.promptengineering.auth.api.dto.VerifyOtpRequest;
import com.promptengineering.auth.api.exception.UnauthorizedException;
import com.promptengineering.auth.application.port.out.OutboxPort;
import com.promptengineering.auth.application.port.out.TokenCachePort;
import com.promptengineering.auth.application.port.out.UserPersistencePort;
import com.promptengineering.auth.application.usecase.AuthUseCaseImpl;
import com.promptengineering.auth.domain.exception.DuplicateEmailException;
import com.promptengineering.auth.domain.exception.InvalidOtpException;
import com.promptengineering.auth.domain.exception.UserNotFoundException;
import com.promptengineering.auth.domain.model.Customer;
import com.promptengineering.auth.domain.model.Store;
import com.promptengineering.auth.domain.model.User;
import com.promptengineering.auth.infrastructure.security.provider.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUseCaseImpl Unit Tests")
class AuthUseCaseImplTest {

    @Mock
    private UserPersistencePort userPersistencePort;

    @Mock
    private TokenCachePort tokenCachePort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private OutboxPort outboxPort;

    @InjectMocks
    private AuthUseCaseImpl authUseCase;

    // ---------------------------------------------------------------------
    // REGISTER
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("A1: register - yeni email ile Customer kaydeder ve OTP yayınlar")
    void register_whenEmailIsNew_shouldSaveCustomerAndPublishOtp() {
        RegisterRequest request = new RegisterRequest("Ali Veli", "ali@example.com", "password123", "CUSTOMER");
        when(userPersistencePort.findByEmail("ali@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-pw");

        authUseCase.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userPersistencePort).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser).isInstanceOf(Customer.class);
        assertThat(savedUser.getRole()).isEqualTo("ROLE_CUSTOMER");
        assertThat(savedUser.getEmail()).isEqualTo("ali@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-pw");
        assertThat(savedUser.isVerified()).isFalse();

        verify(tokenCachePort).saveOtp(eq("ali@example.com"), anyString(), eq(300L));
        verify(outboxPort).publishNotificationEvent(eq("ali@example.com"), anyString());
    }

    @Test
    @DisplayName("A2: register - userType STORE ise Store kaydeder")
    void register_whenUserTypeIsStore_shouldSaveStore() {
        RegisterRequest request = new RegisterRequest("Market X", "store@example.com", "password123", "STORE");
        when(userPersistencePort.findByEmail("store@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-pw");

        authUseCase.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userPersistencePort).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser).isInstanceOf(Store.class);
        assertThat(savedUser.getRole()).isEqualTo("ROLE_STORE");
    }

    @Test
    @DisplayName("A3: register - email zaten varsa DuplicateEmailException fırlatır")
    void register_whenEmailExists_shouldThrowDuplicateEmailException() {
        RegisterRequest request = new RegisterRequest("Ali", "dupe@example.com", "password123", "CUSTOMER");
        User existing = new Customer(UUID.randomUUID(), "Ali", "dupe@example.com", "hash", true, null, null, 0);
        when(userPersistencePort.findByEmail("dupe@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authUseCase.register(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userPersistencePort, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(tokenCachePort);
        verifyNoInteractions(outboxPort);
    }

    // ---------------------------------------------------------------------
    // VERIFY OTP
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("A4: verifyOtp - OTP eşleşirse kullanıcıyı doğrular")
    void verifyOtp_whenOtpMatches_shouldMarkUserVerified() {
        VerifyOtpRequest request = new VerifyOtpRequest("ali@example.com", "123456");
        when(tokenCachePort.getOtp("ali@example.com")).thenReturn(Optional.of("123456"));
        User user = new Customer(UUID.randomUUID(), "Ali", "ali@example.com", "hash", false, null, null, 0);
        when(userPersistencePort.findByEmail("ali@example.com")).thenReturn(Optional.of(user));

        authUseCase.verifyOtp(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userPersistencePort).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isVerified()).isTrue();
        verify(tokenCachePort).deleteOtp("ali@example.com");
    }

    @Test
    @DisplayName("A5: verifyOtp - OTP bulunamazsa InvalidOtpException fırlatır")
    void verifyOtp_whenOtpNotFound_shouldThrowInvalidOtpException() {
        VerifyOtpRequest request = new VerifyOtpRequest("ali@example.com", "123456");
        when(tokenCachePort.getOtp("ali@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authUseCase.verifyOtp(request))
                .isInstanceOf(InvalidOtpException.class);

        verify(userPersistencePort, never()).save(org.mockito.ArgumentMatchers.any());
        verify(tokenCachePort, never()).deleteOtp(anyString());
    }

    @Test
    @DisplayName("A6: verifyOtp - OTP eşleşmezse InvalidOtpException fırlatır")
    void verifyOtp_whenOtpMismatch_shouldThrowInvalidOtpException() {
        VerifyOtpRequest request = new VerifyOtpRequest("ali@example.com", "222222");
        when(tokenCachePort.getOtp("ali@example.com")).thenReturn(Optional.of("111111"));

        assertThatThrownBy(() -> authUseCase.verifyOtp(request))
                .isInstanceOf(InvalidOtpException.class);

        verify(userPersistencePort, never()).save(org.mockito.ArgumentMatchers.any());
        verify(tokenCachePort, never()).deleteOtp(anyString());
    }

    @Test
    @DisplayName("A7: verifyOtp - kullanıcı bulunamazsa UserNotFoundException fırlatır")
    void verifyOtp_whenUserNotFound_shouldThrowUserNotFoundException() {
        VerifyOtpRequest request = new VerifyOtpRequest("ghost@example.com", "123456");
        when(tokenCachePort.getOtp("ghost@example.com")).thenReturn(Optional.of("123456"));
        when(userPersistencePort.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authUseCase.verifyOtp(request))
                .isInstanceOf(UserNotFoundException.class);

        verify(tokenCachePort, never()).deleteOtp(anyString());
    }

    // ---------------------------------------------------------------------
    // LOGIN
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("A8: login - geçerli ve doğrulanmış kullanıcı için token döner")
    void login_whenCredentialsValidAndVerified_shouldReturnToken() {
        LoginRequest request = new LoginRequest("ali@example.com", "password123");
        User user = new Customer(UUID.randomUUID(), "Ali", "ali@example.com", "hashed-pw", true, null, null, 0);
        when(userPersistencePort.findByEmail("ali@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-pw")).thenReturn(true);
        when(jwtTokenProvider.generateToken(user)).thenReturn("mock.jwt.token");

        String result = authUseCase.login(request);

        assertThat(result).isEqualTo("mock.jwt.token");
    }

    @Test
    @DisplayName("A9: login - kullanıcı yoksa UnauthorizedException fırlatır")
    void login_whenUserNotFound_shouldThrowUnauthorizedException() {
        LoginRequest request = new LoginRequest("ghost@example.com", "password123");
        when(userPersistencePort.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authUseCase.login(request))
                .isInstanceOf(UnauthorizedException.class);

        verify(jwtTokenProvider, never()).generateToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("A10: login - parola yanlışsa UnauthorizedException fırlatır")
    void login_whenPasswordWrong_shouldThrowUnauthorizedException() {
        LoginRequest request = new LoginRequest("ali@example.com", "wrong-pw");
        User user = new Customer(UUID.randomUUID(), "Ali", "ali@example.com", "hashed-pw", true, null, null, 0);
        when(userPersistencePort.findByEmail("ali@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pw", "hashed-pw")).thenReturn(false);

        assertThatThrownBy(() -> authUseCase.login(request))
                .isInstanceOf(UnauthorizedException.class);

        verify(jwtTokenProvider, never()).generateToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("A11: login - hesap doğrulanmamışsa UnauthorizedException fırlatır")
    void login_whenNotVerified_shouldThrowUnauthorizedException() {
        LoginRequest request = new LoginRequest("ali@example.com", "password123");
        User user = new Customer(UUID.randomUUID(), "Ali", "ali@example.com", "hashed-pw", false, null, null, 0);
        when(userPersistencePort.findByEmail("ali@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-pw")).thenReturn(true);

        assertThatThrownBy(() -> authUseCase.login(request))
                .isInstanceOf(UnauthorizedException.class);

        verify(jwtTokenProvider, never()).generateToken(org.mockito.ArgumentMatchers.any());
    }
}