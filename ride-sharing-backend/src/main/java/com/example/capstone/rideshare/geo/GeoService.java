package com.example.capstone.rideshare.geo;

public final class GeoService {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoService() {
    }

    public static double distanceKm(double latitudeA, double longitudeA, double latitudeB, double longitudeB) {
        double lat1 = Math.toRadians(latitudeA);
        double lat2 = Math.toRadians(latitudeB);
        double deltaLat = Math.toRadians(latitudeB - latitudeA);
        double deltaLon = Math.toRadians(longitudeB - longitudeA);

        double haversine = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }
}
