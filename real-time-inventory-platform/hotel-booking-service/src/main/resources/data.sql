INSERT INTO cities (id, name, latitude, longitude) VALUES
    (1, 'New York', 40.7128, -74.0060),
    (2, 'Los Angeles', 34.0522, -118.2437);

INSERT INTO hotels (id, name, address, latitude, longitude, deleted, city_id) VALUES
    (1, 'City Center Hotel', '1 Center Plaza, New York, NY', 40.7130, -74.0055, FALSE, 1),
    (2, 'Midtown Stay', '200 Midtown Ave, New York, NY', 40.7580, -73.9855, FALSE, 1),
    (3, 'Brooklyn Lodge', '12 Bridge Street, Brooklyn, NY', 40.6782, -73.9442, FALSE, 1),
    (4, 'Downtown LA Suites', '500 Spring Street, Los Angeles, CA', 34.0500, -118.2500, FALSE, 2),
    (5, 'Deleted Demo Hotel', '99 Hidden Lane, New York, NY', 40.7129, -74.0061, TRUE, 1);
