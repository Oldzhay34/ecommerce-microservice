package com.promptengineering.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank(message = "E-posta boş bırakılamaz")
        @Email(message = "Geçerli bir e-posta adresi giriniz")
        String email,

        @NotBlank(message = "OTP kodu boş bırakılamaz")
        String plainTextOtp
) {}