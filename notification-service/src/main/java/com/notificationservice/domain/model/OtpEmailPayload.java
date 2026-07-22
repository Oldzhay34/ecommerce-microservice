package com.notificationservice.domain.model;

import java.io.Serializable;

public class OtpEmailPayload implements Serializable {
    private String email;
    private String otpCode;

    public OtpEmailPayload() {}

    public OtpEmailPayload(String email, String otpCode) {
        this.email = email;
        this.otpCode = otpCode;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}