package com.example.capstone.notification.delivery;

import com.example.capstone.notification.notification.NotificationRecord;
import java.util.Locale;

final class SimulatedDelivery {

    private SimulatedDelivery() {
    }

    static void deliver(NotificationRecord notification) {
        String payload = (notification.getRecipient() + " " + notification.getSubject() + " " + notification.getBody())
                .toLowerCase(Locale.ROOT);
        if (payload.contains("fail")) {
            throw new DeliveryException("Simulated " + notification.getChannel() + " delivery failure");
        }
    }
}
