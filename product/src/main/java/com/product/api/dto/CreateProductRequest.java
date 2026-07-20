package com.product.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@org.springframework.modulith.NamedInterface("dto")
public record CreateProductRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotNull @Positive BigDecimal price,
        @NotNull @PositiveOrZero Integer stock
) {}