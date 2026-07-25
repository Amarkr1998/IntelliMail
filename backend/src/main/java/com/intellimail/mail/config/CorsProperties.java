package com.intellimail.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "intellimail.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
