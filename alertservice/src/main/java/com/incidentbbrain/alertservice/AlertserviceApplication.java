package com.incidentbbrain.alertservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlertserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertserviceApplication.class, args);
    }

}
