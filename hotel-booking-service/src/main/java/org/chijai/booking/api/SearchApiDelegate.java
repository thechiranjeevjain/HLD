package org.chijai.booking.api;

import java.util.List;
import org.chijai.booking.dto.HotelSearchResponse;

public interface SearchApiDelegate {

    List<HotelSearchResponse> searchClosestHotels(Long cityId);
}
