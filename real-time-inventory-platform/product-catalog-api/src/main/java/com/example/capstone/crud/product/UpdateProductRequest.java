package com.example.capstone.crud.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank
        @Size(max = 64)
        String sku,

        @NotBlank
        @Size(max = 160)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 12, fraction = 2)
        BigDecimal price,

        @NotBlank
        @Pattern(regexp = "[A-Z]{3}", message = "must be an ISO 4217 currency code")
        String currency,

        @NotNull
        @Min(0)
        Integer stockQuantity,

        @NotNull
        Boolean active
) {
}
