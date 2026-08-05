package org.chijai.booking.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "booking.kafka", name = "enabled", havingValue = "true")
public class KafkaHotelEventPublisher implements HotelEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaHotelEventPublisher.class);

    private final KafkaTemplate<String, HotelDeletedEvent> kafkaTemplate;
    private final String topic;

    public KafkaHotelEventPublisher(
            KafkaTemplate<String, HotelDeletedEvent> kafkaTemplate,
            @Value("${booking.kafka.hotel-events-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void hotelDeleted(HotelDeletedEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.hotelId()), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.warn("Failed to publish hotel delete event for hotelId={}", event.hotelId(), exception);
                        return;
                    }
                    log.info("Published hotel delete event for hotelId={} to topic={}", event.hotelId(), topic);
                });
    }
}
