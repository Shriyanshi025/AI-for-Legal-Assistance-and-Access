package com.legalassist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String environment
) {
    public AppProperties {
        if (environment == null || environment.isBlank()) {
            environment = "development";
        }
    }
}
