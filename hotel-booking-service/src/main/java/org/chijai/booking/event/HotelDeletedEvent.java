package org.chijai.booking.event;

import java.time.Instant;

public record HotelDeletedEvent(
        Long hotelId,
        Long cityId,
        Instant deletedAt
) {
}
