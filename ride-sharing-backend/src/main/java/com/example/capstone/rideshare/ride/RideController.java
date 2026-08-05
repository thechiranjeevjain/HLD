package com.example.capstone.rideshare.ride;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    public RideResponse request(@Valid @RequestBody RideRequest request) {
        return rideService.request(request);
    }

    @GetMapping("/{id}")
    public RideResponse get(@PathVariable UUID id) {
        return rideService.get(id);
    }

    @PatchMapping("/{id}/status")
    public RideResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateRideStatusRequest request) {
        return rideService.updateStatus(id, request);
    }
}
