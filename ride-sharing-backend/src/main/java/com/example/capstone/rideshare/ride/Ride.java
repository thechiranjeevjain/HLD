package com.example.capstone.rideshare.ride;

import com.example.capstone.rideshare.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String riderId;

    @Column(nullable = false)
    private UUID driverId;

    @Column(nullable = false)
    private double pickupLatitude;

    @Column(nullable = false)
    private double pickupLongitude;

    @Column(nullable = false)
    private double dropoffLatitude;

    @Column(nullable = false)
    private double dropoffLongitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RideStatus status = RideStatus.MATCHED;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fareEstimate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Ride() {
    }

    public Ride(
            String riderId,
            UUID driverId,
            double pickupLatitude,
            double pickupLongitude,
            double dropoffLatitude,
            double dropoffLongitude,
            BigDecimal fareEstimate
    ) {
        this.riderId = riderId;
        this.driverId = driverId;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.fareEstimate = fareEstimate;
    }

    public void transitionTo(RideStatus nextStatus) {
        if (status == RideStatus.COMPLETED || status == RideStatus.CANCELLED) {
            throw new DomainException("Terminal rides cannot change status");
        }
        if (nextStatus == RideStatus.MATCHED && status != RideStatus.MATCHED) {
            throw new DomainException("Ride cannot move back to MATCHED");
        }
        this.status = nextStatus;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getRiderId() {
        return riderId;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public double getPickupLatitude() {
        return pickupLatitude;
    }

    public double getPickupLongitude() {
        return pickupLongitude;
    }

    public double getDropoffLatitude() {
        return dropoffLatitude;
    }

    public double getDropoffLongitude() {
        return dropoffLongitude;
    }

    public RideStatus getStatus() {
        return status;
    }

    public BigDecimal getFareEstimate() {
        return fareEstimate;
    }
}
