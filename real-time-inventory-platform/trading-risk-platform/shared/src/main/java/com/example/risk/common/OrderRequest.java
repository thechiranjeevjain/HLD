package com.example.risk.common;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank String clientId,
        @NotBlank String symbol,
        @NotNull OrderSide side,
        @Positive long quantity,
        @NotNull @DecimalMin("0.01") BigDecimal price
) {
    public BigDecimal notional() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}

