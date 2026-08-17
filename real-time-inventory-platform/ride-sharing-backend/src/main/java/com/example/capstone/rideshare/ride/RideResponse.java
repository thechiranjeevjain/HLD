package com.example.capstone.rideshare.ride;

import java.math.BigDecimal;
import java.util.UUID;

public record RideResponse(
        UUID id,
        String riderId,
        UUID driverId,
        RideStatus status,
        double pickupLatitude,
        double pickupLongitude,
        double dropoffLatitude,
        double dropoffLongitude,
        BigDecimal fareEstimate
) {

    public static RideResponse from(Ride ride) {
        return new RideResponse(
                ride.getId(),
                ride.getRiderId(),
                ride.getDriverId(),
                ride.getStatus(),
                ride.getPickupLatitude(),
                ride.getPickupLongitude(),
                ride.getDropoffLatitude(),
                ride.getDropoffLongitude(),
                ride.getFareEstimate()
        );
    }
}
