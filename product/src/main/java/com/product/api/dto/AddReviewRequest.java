package com.product.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@org.springframework.modulith.NamedInterface("dto")
public record AddReviewRequest(
        @NotNull UUID productId,
        @NotBlank String comment,
        @NotNull @Min(1) @Max(5) Integer rating
) {}