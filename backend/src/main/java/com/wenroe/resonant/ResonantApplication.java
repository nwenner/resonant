package com.wenroe.resonant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@SpringBootApplication
@EnableR2dbcAuditing  // Changed from EnableJpaAuditing
public class ResonantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResonantApplication.class, args);
    }
}