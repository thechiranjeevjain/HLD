package com.example.capstone.rideshare.driver;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    public DriverResponse create(@Valid @RequestBody CreateDriverRequest request) {
        return driverService.create(request);
    }

    @PatchMapping("/{id}/location")
    public DriverResponse updateLocation(@PathVariable UUID id, @Valid @RequestBody UpdateDriverLocationRequest request) {
        return driverService.updateLocation(id, request);
    }

    @GetMapping("/nearby")
    public List<NearbyDriverResponse> nearby(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @RequestParam(defaultValue = "5") @DecimalMin("0.1") double radiusKm
    ) {
        return driverService.nearby(latitude, longitude, radiusKm);
    }
}
