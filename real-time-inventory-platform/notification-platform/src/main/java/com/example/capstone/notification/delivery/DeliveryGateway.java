package com.example.capstone.notification.delivery;

import com.example.capstone.notification.notification.NotificationChannel;
import com.example.capstone.notification.notification.NotificationRecord;

public interface DeliveryGateway {

    NotificationChannel channel();

    void deliver(NotificationRecord notification);
}
