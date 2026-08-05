# Ride-Sharing Backend Interview Guide

## Two-Minute Pitch

This service models the backend core of a ride-sharing system: driver registration, location updates, nearby-driver search, ride matching, ride lifecycle transitions, and WebSocket updates. It is useful because it combines geospatial reasoning with workflow state.

## What To Emphasize

- Driver availability and location are separate pieces of state.
- Nearby search uses a distance calculation and radius filter.
- Matching chooses an available driver and creates a ride.
- Ride status transitions should be controlled by a state machine.
- WebSocket updates demonstrate push-style client notifications.

## Request Flow

1. Driver registers and updates current coordinates.
2. Rider posts pickup, dropoff, and search radius.
3. `GeoService` finds nearby available drivers.
4. `RideService` assigns a driver and persists the ride.
5. Status updates move the ride through lifecycle states.
6. WebSocket subscribers can receive ride updates.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| Simple radius search | Easy to inspect and test | Does not scale like geohash or spatial index |
| PostgreSQL persistence | Durable drivers and rides | Real-time location needs faster update paths |
| WebSocket notifications | Better live UX | Requires connection management |
| Single service | Clear interview walkthrough | Real systems split dispatch, pricing, maps, payments, and notifications |

## FAQ

Q: How would this scale beyond a few drivers?
A: Use geospatial indexing, partition by city/region, cache hot driver locations, and separate high-frequency location writes from ride state.

Q: What is the key consistency issue?
A: Avoid assigning the same driver to two rides. Driver availability and ride assignment must be updated atomically.

Q: What would you add next?
A: fare estimation, cancellation rules, driver heartbeats, ETA service, location streams, and idempotency for ride requests.
