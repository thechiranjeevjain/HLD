package com.example.risk.history;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class HistoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(HistoryServiceApplication.class, args);
    }
}
