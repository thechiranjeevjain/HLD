package com.example.capstone.ecommerce.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
        @NotBlank
        @Size(max = 160)
        String paymentToken
) {
}
