package com.example.capstone.rideshare.ride;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideRepository extends JpaRepository<Ride, UUID> {
}
