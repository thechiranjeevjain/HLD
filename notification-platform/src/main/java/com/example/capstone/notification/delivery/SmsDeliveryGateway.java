package com.example.capstone.notification.delivery;

import com.example.capstone.notification.notification.NotificationChannel;
import com.example.capstone.notification.notification.NotificationRecord;
import org.springframework.stereotype.Component;

@Component
public class SmsDeliveryGateway implements DeliveryGateway {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void deliver(NotificationRecord notification) {
        SimulatedDelivery.deliver(notification);
    }
}
