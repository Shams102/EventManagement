package com.campus.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventManagementApplication {

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        System.out.println("========================================");
        System.out.println("PORT env variable: " + port);
        System.out.println("Binding to port: " + (port != null ? port : "8080 (default)"));
        System.out.println("========================================");
        SpringApplication.run(EventManagementApplication.class, args);
    }
}
