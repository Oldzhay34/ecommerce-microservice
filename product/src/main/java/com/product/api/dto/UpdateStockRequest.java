package com.product.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@org.springframework.modulith.NamedInterface("dto")
public record UpdateStockRequest(
        @NotNull @PositiveOrZero Integer newStock
) {}