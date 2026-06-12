package com.incidentbbrain.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {

        System.out.println("SERVER_IP = " + System.getenv("SERVER_IP"));

        SpringApplication.run(AuthServiceApplication.class, args);
    }

}