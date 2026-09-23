package com.legalassist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public record SupabaseStorageProperties(
        String url,
        String key,
        StorageProperties storage
) {
    public record StorageProperties(
            String bucket
    ) {
        public StorageProperties {
            if (bucket == null || bucket.isBlank()) {
                bucket = "documents";
            }
        }
    }
}
