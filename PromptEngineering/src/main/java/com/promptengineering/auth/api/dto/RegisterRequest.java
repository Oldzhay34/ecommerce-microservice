package com.promptengineering.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "İsim boş bırakılamaz")
        String name,

        @NotBlank(message = "E-posta boş bırakılamaz")
        @Email(message = "Geçerli bir e-posta adresi giriniz")
        String email,

        @NotBlank(message = "Parola boş bırakılamaz")
        @Size(min = 8, message = "Parola en az 8 karakter olmalıdır")
        String password,

        @NotBlank(message = "Kullanıcı tipi boş bırakılamaz")
        @Pattern(regexp = "^(CUSTOMER|STORE)$", message = "Kullanıcı tipi sadece CUSTOMER veya STORE olabilir")
        String userType
) {}