package com.fleetx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FleetxApplication {

    public static void main(String[] args) {
        SpringApplication.run(FleetxApplication.class, args);
    }
}
