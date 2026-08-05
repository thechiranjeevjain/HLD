package org.chijai.booking.api;

import org.chijai.booking.dto.HotelResponse;

public interface HotelApiDelegate {

    HotelResponse getHotel(Long id);

    void deleteHotel(Long id);
}
