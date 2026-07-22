package com.review.api.dto;

import jakarta.validation.constraints.NotBlank;

public class StoreReplyRequest {
    @NotBlank(message = "Reply text cannot be empty")
    private String replyText;

    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }
}