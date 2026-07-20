package com.promptengineering.auth.api.controller;

import com.promptengineering.auth.api.dto.LoginRequest;
import com.promptengineering.auth.api.dto.RegisterRequest;
import com.promptengineering.auth.api.dto.VerifyOtpRequest;
import com.promptengineering.auth.application.port.in.AuthUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
// NOT: @CrossOrigin anotasyonu Gateway üzerinden yönetildiği için kaldırıldı.
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authUseCase.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Kayıt başarılı. Lütfen e-posta adresinize gelen kodu giriniz."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authUseCase.verifyOtp(request);
        return ResponseEntity.ok(Map.of("message", "E-posta doğrulama işlemi başarıyla tamamlandı."));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        String token = authUseCase.login(request);
        return ResponseEntity.ok(Map.of(
                "message", "Giriş başarılı.",
                "accessToken", token
        ));
    }
}