package com.example.capstone.rideshare.ride;

import com.example.capstone.rideshare.driver.Driver;
import com.example.capstone.rideshare.driver.DriverRepository;
import com.example.capstone.rideshare.driver.DriverStatus;
import com.example.capstone.rideshare.error.DomainException;
import com.example.capstone.rideshare.error.NotFoundException;
import com.example.capstone.rideshare.geo.GeoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RideService {

    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public RideService(DriverRepository driverRepository, RideRepository rideRepository, SimpMessagingTemplate messagingTemplate) {
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public RideResponse request(RideRequest request) {
        double radiusKm = request.radiusKm() == null ? 5.0 : request.radiusKm();
        Driver driver = driverRepository.findByStatus(DriverStatus.AVAILABLE).stream()
                .filter(candidate -> GeoService.distanceKm(
                        request.pickupLatitude(),
                        request.pickupLongitude(),
                        candidate.getLatitude(),
                        candidate.getLongitude()) <= radiusKm)
                .min(Comparator.comparingDouble(candidate -> GeoService.distanceKm(
                        request.pickupLatitude(),
                        request.pickupLongitude(),
                        candidate.getLatitude(),
                        candidate.getLongitude())))
                .orElseThrow(() -> new DomainException("No available drivers within " + radiusKm + " km"));

        driver.markBusy();
        Ride ride = new Ride(
                request.riderId().trim(),
                driver.getId(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.dropoffLatitude(),
                request.dropoffLongitude(),
                estimateFare(request)
        );
        RideResponse response = RideResponse.from(rideRepository.save(ride));
        broadcast(response);
        return response;
    }

    @Transactional(readOnly = true)
    public RideResponse get(UUID id) {
        return rideRepository.findById(id)
                .map(RideResponse::from)
                .orElseThrow(() -> new NotFoundException("Ride not found: " + id));
    }

    public RideResponse updateStatus(UUID id, UpdateRideStatusRequest request) {
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ride not found: " + id));
        ride.transitionTo(request.status());
        if (request.status() == RideStatus.COMPLETED || request.status() == RideStatus.CANCELLED) {
            driverRepository.findById(ride.getDriverId()).ifPresent(Driver::markAvailable);
        }
        RideResponse response = RideResponse.from(ride);
        broadcast(response);
        return response;
    }

    private BigDecimal estimateFare(RideRequest request) {
        double km = GeoService.distanceKm(
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.dropoffLatitude(),
                request.dropoffLongitude()
        );
        return BigDecimal.valueOf(4.50 + (km * 1.75)).setScale(2, RoundingMode.HALF_UP);
    }

    private void broadcast(RideResponse response) {
        messagingTemplate.convertAndSend("/topic/rides/" + response.id(), response);
    }
}
