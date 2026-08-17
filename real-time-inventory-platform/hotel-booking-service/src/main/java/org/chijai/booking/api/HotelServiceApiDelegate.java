package org.chijai.booking.api;

import java.util.List;
import org.chijai.booking.dto.HotelResponse;
import org.chijai.booking.dto.HotelSearchResponse;
import org.chijai.booking.service.HotelService;
import org.springframework.stereotype.Component;

@Component
public class HotelServiceApiDelegate implements HotelApiDelegate, SearchApiDelegate {

    private final HotelService hotelService;

    public HotelServiceApiDelegate(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @Override
    public HotelResponse getHotel(Long id) {
        return hotelService.getHotel(id);
    }

    @Override
    public void deleteHotel(Long id) {
        hotelService.softDeleteHotel(id);
    }

    @Override
    public List<HotelSearchResponse> searchClosestHotels(Long cityId) {
        return hotelService.searchClosestHotels(cityId);
    }
}
