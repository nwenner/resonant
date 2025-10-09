package com.wenroe.resonant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ResonantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResonantApplication.class, args);
    }
}