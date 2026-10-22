package com.disaster.response;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI-Based Disaster Prediction & Emergency Response System
 * Production-minded backend supporting real-time disaster alerts,
 * terrain & GIS processing, ML risk inference, and admin-approved emergency dispatch.
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class DisasterEmergencyApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisasterEmergencyApplication.class, args);
    }
}
