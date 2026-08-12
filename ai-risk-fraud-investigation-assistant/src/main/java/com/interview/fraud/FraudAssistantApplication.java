package com.interview.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FraudAssistantApplication {
    public static void main(String[] args) { SpringApplication.run(FraudAssistantApplication.class, args); }
}
