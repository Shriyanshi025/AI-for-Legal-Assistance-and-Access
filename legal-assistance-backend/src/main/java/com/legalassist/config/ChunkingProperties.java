package com.legalassist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chunking")
public record ChunkingProperties(
        int maxChunkSize,
        int overlapSize
) {
    public ChunkingProperties {
        if (maxChunkSize <= 0) {
            maxChunkSize = 1500;
        }
        if (overlapSize < 0) {
            overlapSize = 200;
        }
        if (overlapSize >= maxChunkSize) {
            throw new IllegalArgumentException("Overlap size must be strictly smaller than max chunk size");
        }
    }
}
