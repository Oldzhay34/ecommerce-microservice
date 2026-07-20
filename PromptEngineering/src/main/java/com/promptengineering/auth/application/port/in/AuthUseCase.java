package com.promptengineering.auth.application.port.in;

import com.promptengineering.auth.api.dto.LoginRequest;
import com.promptengineering.auth.api.dto.RegisterRequest;
import com.promptengineering.auth.api.dto.VerifyOtpRequest;

public interface AuthUseCase {
    void register(RegisterRequest request);
    void verifyOtp(VerifyOtpRequest request);
    String login(LoginRequest request);
}
