package com.intellimail.mail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the IntelliMail backend service.
 * Bootstraps Spring context, component scanning and configuration property binding.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class IntelliMailApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelliMailApplication.class, args);
    }
}
