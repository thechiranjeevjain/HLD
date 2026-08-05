package org.chijai.booking.dto;

import java.io.Serializable;

public record HotelResponse(
        Long id,
        String name,
        String address,
        double latitude,
        double longitude,
        Long cityId,
        String cityName,
        boolean deleted
) implements Serializable {
}
