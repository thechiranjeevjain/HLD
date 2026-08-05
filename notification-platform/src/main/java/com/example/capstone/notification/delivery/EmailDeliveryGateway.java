package com.example.capstone.notification.delivery;

import com.example.capstone.notification.notification.NotificationChannel;
import com.example.capstone.notification.notification.NotificationRecord;
import org.springframework.stereotype.Component;

@Component
public class EmailDeliveryGateway implements DeliveryGateway {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void deliver(NotificationRecord notification) {
        SimulatedDelivery.deliver(notification);
    }
}
