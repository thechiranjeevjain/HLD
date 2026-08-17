package com.example.capstone.notification.delivery;

import com.example.capstone.notification.notification.NotificationChannel;
import com.example.capstone.notification.notification.NotificationRecord;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DeliveryRouter {

    private final Map<NotificationChannel, DeliveryGateway> gateways = new EnumMap<>(NotificationChannel.class);

    public DeliveryRouter(List<DeliveryGateway> gateways) {
        gateways.forEach(gateway -> this.gateways.put(gateway.channel(), gateway));
    }

    public void deliver(NotificationRecord notification) {
        DeliveryGateway gateway = gateways.get(notification.getChannel());
        if (gateway == null) {
            throw new DeliveryException("No gateway configured for " + notification.getChannel());
        }
        gateway.deliver(notification);
    }
}
