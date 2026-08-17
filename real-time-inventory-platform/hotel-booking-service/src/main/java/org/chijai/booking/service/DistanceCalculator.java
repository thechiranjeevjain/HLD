package org.chijai.booking.service;

import org.springframework.stereotype.Component;

@Component
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public double haversineInKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double lat1Radians = Math.toRadians(lat1);
        double lat2Radians = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1Radians)
                * Math.cos(lat2Radians)
                * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return EARTH_RADIUS_KM * c;
    }
}
