package org.chijai.booking.event;

public interface HotelEventPublisher {

    void hotelDeleted(HotelDeletedEvent event);
}
