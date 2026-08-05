package com.example.capstone.rideshare.driver;

import java.util.UUID;

public record DriverResponse(
        UUID id,
        String name,
        DriverStatus status,
        double latitude,
        double longitude
) {

    public static DriverResponse from(Driver driver) {
        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getStatus(),
                driver.getLatitude(),
                driver.getLongitude()
        );
    }
}
