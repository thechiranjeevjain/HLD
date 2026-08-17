package org.chijai.booking.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "booking.kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpHotelEventPublisher implements HotelEventPublisher {

    @Override
    public void hotelDeleted(HotelDeletedEvent event) {
    }
}
