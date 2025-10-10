package com.wenroe.resonant.config;

import org.springframework.context.annotation.Configuration;

/**
 * Flyway configuration for database migrations.
 * With Spring Boot MVC and JDBC, Flyway auto-configuration works out of the box.
 * This class is kept for future custom Flyway configurations if needed.
 */
@Configuration
public class FlywayConfig {
    // Flyway is automatically configured via application.yml
    // No custom configuration needed for standard JDBC setup
}