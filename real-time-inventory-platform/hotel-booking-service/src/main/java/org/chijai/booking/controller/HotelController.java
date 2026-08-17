package org.chijai.booking.controller;

import java.util.List;
import org.chijai.booking.api.HotelApiDelegate;
import org.chijai.booking.api.SearchApiDelegate;
import org.chijai.booking.dto.HotelResponse;
import org.chijai.booking.dto.HotelSearchResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HotelController {

    private final HotelApiDelegate hotelApiDelegate;
    private final SearchApiDelegate searchApiDelegate;

    public HotelController(HotelApiDelegate hotelApiDelegate, SearchApiDelegate searchApiDelegate) {
        this.hotelApiDelegate = hotelApiDelegate;
        this.searchApiDelegate = searchApiDelegate;
    }

    @GetMapping("/hotel/{id}")
    public HotelResponse getHotel(@PathVariable Long id) {
        return hotelApiDelegate.getHotel(id);
    }

    @DeleteMapping("/hotel/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id) {
        hotelApiDelegate.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/{cityId}")
    public List<HotelSearchResponse> searchClosestHotels(@PathVariable Long cityId) {
        return searchApiDelegate.searchClosestHotels(cityId);
    }
}
