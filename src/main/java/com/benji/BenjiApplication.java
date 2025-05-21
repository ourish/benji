package com.benji;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BenjiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BenjiApplication.class, args);
    }
}