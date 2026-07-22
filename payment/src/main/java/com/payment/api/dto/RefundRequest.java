package com.payment.api.dto;

import jakarta.validation.constraints.NotBlank;

public class RefundRequest {

    @NotBlank(message = "Refund reason cannot be empty")
    private String reason;

    public RefundRequest() {
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}