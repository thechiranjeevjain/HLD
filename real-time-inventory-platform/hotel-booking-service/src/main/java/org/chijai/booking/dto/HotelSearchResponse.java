package org.chijai.booking.dto;

import java.io.Serializable;

public record HotelSearchResponse(
        Long id,
        String name,
        String address,
        double latitude,
        double longitude,
        Long cityId,
        String cityName,
        double distanceFromCityCenterKm
) implements Serializable {
}
