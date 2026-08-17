package com.example.capstone.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NotificationPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationPlatformApplication.class, args);
    }
}
