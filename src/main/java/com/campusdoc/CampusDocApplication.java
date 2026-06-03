package com.campusdoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CampusDocApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusDocApplication.class, args);
    }
}
