package com.example.capstone.rideshare.driver;

import java.util.UUID;

public record NearbyDriverResponse(
        UUID id,
        String name,
        double latitude,
        double longitude,
        double distanceKm
) {

    public static NearbyDriverResponse from(Driver driver, double distanceKm) {
        return new NearbyDriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getLatitude(),
                driver.getLongitude(),
                distanceKm
        );
    }
}
