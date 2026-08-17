package org.chijai.booking.service;

import org.chijai.booking.domain.City;
import org.chijai.booking.domain.Hotel;
import org.chijai.booking.dto.HotelResponse;
import org.chijai.booking.dto.HotelSearchResponse;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {

    public HotelResponse toResponse(Hotel hotel) {
        City city = hotel.getCity();
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getAddress(),
                hotel.getLatitude(),
                hotel.getLongitude(),
                city.getId(),
                city.getName(),
                hotel.isDeleted()
        );
    }

    public HotelSearchResponse toSearchResponse(Hotel hotel, double distanceFromCityCenterKm) {
        City city = hotel.getCity();
        return new HotelSearchResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getAddress(),
                hotel.getLatitude(),
                hotel.getLongitude(),
                city.getId(),
                city.getName(),
                distanceFromCityCenterKm
        );
    }
}
