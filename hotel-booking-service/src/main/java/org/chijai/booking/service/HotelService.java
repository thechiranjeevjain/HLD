package org.chijai.booking.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.chijai.booking.domain.City;
import org.chijai.booking.domain.Hotel;
import org.chijai.booking.dto.HotelResponse;
import org.chijai.booking.dto.HotelSearchResponse;
import org.chijai.booking.event.HotelDeletedEvent;
import org.chijai.booking.event.HotelEventPublisher;
import org.chijai.booking.exception.ResourceNotFoundException;
import org.chijai.booking.repository.CityRepository;
import org.chijai.booking.repository.HotelRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelService {

    private final HotelRepository hotelRepository;
    private final CityRepository cityRepository;
    private final HotelMapper hotelMapper;
    private final DistanceCalculator distanceCalculator;
    private final HotelEventPublisher hotelEventPublisher;

    public HotelService(
            HotelRepository hotelRepository,
            CityRepository cityRepository,
            HotelMapper hotelMapper,
            DistanceCalculator distanceCalculator,
            HotelEventPublisher hotelEventPublisher
    ) {
        this.hotelRepository = hotelRepository;
        this.cityRepository = cityRepository;
        this.hotelMapper = hotelMapper;
        this.distanceCalculator = distanceCalculator;
        this.hotelEventPublisher = hotelEventPublisher;
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotel(Long id) {
        Hotel hotel = hotelRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        return hotelMapper.toResponse(hotel);
    }

    @Transactional
    @CacheEvict(cacheNames = "closestHotels", allEntries = true)
    public void softDeleteHotel(Long id) {
        Hotel hotel = hotelRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        hotel.markDeleted();
        hotelEventPublisher.hotelDeleted(new HotelDeletedEvent(hotel.getId(), hotel.getCity().getId(), Instant.now()));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "closestHotels", key = "#cityId")
    public List<HotelSearchResponse> searchClosestHotels(Long cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + cityId));

        return hotelRepository.findByCityIdAndDeletedFalse(cityId).stream()
                .map(hotel -> toSearchResponse(city, hotel))
                .sorted(Comparator.comparingDouble(HotelSearchResponse::distanceFromCityCenterKm))
                .toList();
    }

    private HotelSearchResponse toSearchResponse(City city, Hotel hotel) {
        double distance = distanceCalculator.haversineInKm(
                city.getLatitude(),
                city.getLongitude(),
                hotel.getLatitude(),
                hotel.getLongitude()
        );
        return hotelMapper.toSearchResponse(hotel, distance);
    }
}
