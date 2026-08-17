package com.example.capstone.rideshare.ride;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RideRequest(
        @NotBlank
        @Size(max = 160)
        String riderId,

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double pickupLatitude,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double pickupLongitude,

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double dropoffLatitude,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double dropoffLongitude,

        @DecimalMin("0.1")
        Double radiusKm
) {
}
