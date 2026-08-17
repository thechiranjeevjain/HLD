package com.example.capstone.rideshare.geo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeoServiceTest {

    @Test
    void distanceBetweenNearbyManhattanPointsIsSmall() {
        double distance = GeoService.distanceKm(40.758, -73.9855, 40.7614, -73.9776);

        assertThat(distance).isBetween(0.6, 0.9);
    }

    @Test
    void distanceIsZeroForSamePoint() {
        assertThat(GeoService.distanceKm(40.758, -73.9855, 40.758, -73.9855)).isZero();
    }
}
