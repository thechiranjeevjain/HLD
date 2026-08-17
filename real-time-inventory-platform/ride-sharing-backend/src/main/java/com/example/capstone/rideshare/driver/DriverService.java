package com.example.capstone.rideshare.driver;

import com.example.capstone.rideshare.error.NotFoundException;
import com.example.capstone.rideshare.geo.GeoService;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public DriverResponse create(CreateDriverRequest request) {
        return DriverResponse.from(driverRepository.save(new Driver(
                request.name().trim(),
                request.latitude(),
                request.longitude()
        )));
    }

    public DriverResponse updateLocation(UUID id, UpdateDriverLocationRequest request) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + id));
        driver.updateLocation(request.latitude(), request.longitude(), request.status());
        return DriverResponse.from(driver);
    }

    @Transactional(readOnly = true)
    public List<NearbyDriverResponse> nearby(double latitude, double longitude, double radiusKm) {
        return driverRepository.findByStatus(DriverStatus.AVAILABLE).stream()
                .map(driver -> NearbyDriverResponse.from(driver, GeoService.distanceKm(latitude, longitude, driver.getLatitude(), driver.getLongitude())))
                .filter(driver -> driver.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyDriverResponse::distanceKm))
                .toList();
    }
}
