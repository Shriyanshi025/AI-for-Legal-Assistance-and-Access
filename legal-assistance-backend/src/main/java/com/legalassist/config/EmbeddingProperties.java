package com.legalassist.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.embedding")
public class EmbeddingProperties {

    private String provider = "google-gemini";
    private String model = "gemini-embedding-001";
    private int dimension = 768;

    @Value("${gemini.api.key:}")
    private String apiKey;

    public EmbeddingProperties() {
    }

    public EmbeddingProperties(String provider, String model, int dimension, String apiKey) {
        this.provider = provider;
        this.model = model;
        this.dimension = dimension;
        this.apiKey = apiKey;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
