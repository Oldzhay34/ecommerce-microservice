package com.review.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ModerateReviewRequest {
    // BUGFIX: Sadece @Pattern vardı. Bean Validation sözleşmesi gereği
    // @Pattern null değeri GEÇERLİ sayar; bu yüzden {"status": null} (veya
    // alanı hiç göndermemek) validasyondan geçiyor ve
    // ReviewStatus.valueOf(null) çağrısı NullPointerException -> HTTP 500
    // üretiyordu. Doğru davranış 400 Bad Request olmalı.
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|HIDDEN)$", message = "Status must be ACTIVE or HIDDEN")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}