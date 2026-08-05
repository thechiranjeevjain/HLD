package com.example.capstone.rideshare.ride;

import jakarta.validation.constraints.NotNull;

public record UpdateRideStatusRequest(
        @NotNull
        RideStatus status
) {
}
