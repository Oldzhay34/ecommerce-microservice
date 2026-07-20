package com.promptengineering.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptengineering.auth.api.controller.AuthController;
import com.promptengineering.auth.api.dto.LoginRequest;
import com.promptengineering.auth.api.dto.RegisterRequest;
import com.promptengineering.auth.api.dto.VerifyOtpRequest;
import com.promptengineering.auth.api.exception.GlobalExceptionHandler;
import com.promptengineering.auth.api.exception.UnauthorizedException;
import com.promptengineering.auth.application.port.in.AuthUseCase;
import com.promptengineering.auth.infrastructure.security.provider.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ÇÖZÜM UYGULANDI: Spring'den beklemek yerine nesneyi doğrudan oluşturuyoruz.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthUseCase authUseCase;

    // JWT Filter'in calisabilmesi (context'in patlamamasi) icin eklenen mock
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("B1: register - gecerli body ile 201 ve mesaj doner")
    void register_withValidBody_shouldReturn201AndMessage() throws Exception {
        RegisterRequest request = new RegisterRequest("Ali Veli", "ali@example.com", "password123", "CUSTOMER");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        verify(authUseCase).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("B2: register - gecersiz email ile 400 doner")
    void register_withInvalidEmail_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("Ali Veli", "invalid-email", "password123", "CUSTOMER");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authUseCase, never()).register(any());
    }

    @Test
    @DisplayName("B3: register - kisa parola ile 400 doner")
    void register_withShortPassword_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("Ali Veli", "ali@example.com", "short", "CUSTOMER");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authUseCase, never()).register(any());
    }

    @Test
    @DisplayName("B4: register - gecersiz userType ile 400 doner")
    void register_withInvalidUserType_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("Ali Veli", "ali@example.com", "password123", "ADMIN");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authUseCase, never()).register(any());
    }

    @Test
    @DisplayName("B5: verifyOtp - gecerli body ile 200 doner")
    void verifyOtp_withValidBody_shouldReturn200() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("ali@example.com", "123456");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(authUseCase).verifyOtp(any(VerifyOtpRequest.class));
    }

    @Test
    @DisplayName("B6: login - gecerli body ile 200 ve accessToken doner")
    void login_withValidBody_shouldReturn200AndAccessToken() throws Exception {
        LoginRequest request = new LoginRequest("ali@example.com", "password123");
        when(authUseCase.login(any(LoginRequest.class))).thenReturn("jwt.token.here");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.here"));

        verify(authUseCase).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("B7: login - UseCase UnauthorizedException firlatirsa 401 doner")
    void login_whenUseCaseThrowsUnauthorized_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("ali@example.com", "wrong-pw");
        when(authUseCase.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("E-posta veya parola hatali."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}