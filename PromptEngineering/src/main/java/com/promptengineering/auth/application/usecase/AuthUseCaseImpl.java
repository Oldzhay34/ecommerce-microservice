package com.promptengineering.auth.application.usecase;

import com.promptengineering.auth.api.dto.LoginRequest;
import com.promptengineering.auth.api.dto.RegisterRequest;
import com.promptengineering.auth.api.dto.VerifyOtpRequest;
import com.promptengineering.auth.application.port.in.AuthUseCase;
import com.promptengineering.auth.application.port.out.OutboxPort;
import com.promptengineering.auth.application.port.out.TokenCachePort;
import com.promptengineering.auth.application.port.out.UserPersistencePort;
import com.promptengineering.auth.api.exception.UnauthorizedException;
import com.promptengineering.auth.domain.exception.DuplicateEmailException;
import com.promptengineering.auth.domain.exception.InvalidOtpException;
import com.promptengineering.auth.domain.exception.UserNotFoundException;
import com.promptengineering.auth.domain.model.Customer;
import com.promptengineering.auth.domain.model.Store;
import com.promptengineering.auth.domain.model.User;
import com.promptengineering.auth.infrastructure.security.provider.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthUseCaseImpl implements AuthUseCase {

    private final UserPersistencePort userPersistencePort;
    private final TokenCachePort tokenCachePort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OutboxPort outboxPort;

    public AuthUseCaseImpl(UserPersistencePort userPersistencePort,
                           TokenCachePort tokenCachePort,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           OutboxPort outboxPort) {
        this.userPersistencePort = userPersistencePort;
        this.tokenCachePort = tokenCachePort;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.outboxPort = outboxPort;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        Optional<User> existingUser = userPersistencePort.findByEmail(request.email());
        if (existingUser.isPresent()) {
            throw new DuplicateEmailException("Bu e-posta adresi ile zaten bir kayıt mevcut.");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User newUser;

        // Gelen userType bilgisine göre ilgili Domain nesnesini üretiyoruz.
        if ("STORE".equalsIgnoreCase(request.userType())) {
            newUser = new Store(
                    UUID.randomUUID(),
                    request.name(),
                    request.email(),
                    hashedPassword,
                    false, // isVerified
                    request.name(), // storeName olarak varsayılan isim eklenebilir veya null bırakılabilir
                    null, // taxNumber sonradan profilden doldurulabilir
                    0.0   // storeRating başlangıç
            );
        } else {
            // Varsayılan olarak Müşteri (Customer) hesabı açılır
            newUser = new Customer(
                    UUID.randomUUID(),
                    request.name(),
                    request.email(),
                    hashedPassword,
                    false, // isVerified
                    null, // shippingAddress sonradan doldurulabilir
                    null, // phoneNumber
                    0     // loyaltyPoints başlangıç
            );
        }

        userPersistencePort.save(newUser);

        String otpCode = generatePlainOtp();
        long expirationSeconds = 300L; // 5 dakika
        tokenCachePort.saveOtp(request.email(), otpCode, expirationSeconds);

        // GÜNCELLEME: Transactional Outbox Pattern ile RabbitMQ'ya iletilmek üzere DB'ye yazılır.
        outboxPort.publishNotificationEvent(request.email(), otpCode);
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        String cachedOtp = tokenCachePort.getOtp(request.email())
                .orElseThrow(() -> new InvalidOtpException("OTP kodu bulunamadı veya süresi dolmuş."));

        if (!cachedOtp.equals(request.plainTextOtp())) {
            throw new InvalidOtpException("Girilen OTP kodu hatalı.");
        }

        User user = userPersistencePort.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException("Kullanıcı bulunamadı."));

        user.setVerified(true);
        userPersistencePort.save(user);

        tokenCachePort.deleteOtp(request.email());
    }

    @Override
    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        User user = userPersistencePort.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("E-posta veya parola hatalı."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("E-posta veya parola hatalı.");
        }

        if (!user.isVerified()) {
            throw new UnauthorizedException("Hesabınız henüz doğrulanmamış. Lütfen e-posta adresinize gönderilen düz metin OTP kodu ile doğrulama yapınız.");
        }

        return jwtTokenProvider.generateToken(user);
    }

    private String generatePlainOtp() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}