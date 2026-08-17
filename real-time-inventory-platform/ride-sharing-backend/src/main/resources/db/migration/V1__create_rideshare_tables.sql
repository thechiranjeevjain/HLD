CREATE TABLE drivers (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE rides (
    id UUID PRIMARY KEY,
    rider_id VARCHAR(160) NOT NULL,
    driver_id UUID NOT NULL REFERENCES drivers (id),
    pickup_latitude DOUBLE PRECISION NOT NULL,
    pickup_longitude DOUBLE PRECISION NOT NULL,
    dropoff_latitude DOUBLE PRECISION NOT NULL,
    dropoff_longitude DOUBLE PRECISION NOT NULL,
    status VARCHAR(32) NOT NULL,
    fare_estimate NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_drivers_status ON drivers (status);
CREATE INDEX idx_rides_rider_id ON rides (rider_id);
CREATE INDEX idx_rides_driver_id ON rides (driver_id);
