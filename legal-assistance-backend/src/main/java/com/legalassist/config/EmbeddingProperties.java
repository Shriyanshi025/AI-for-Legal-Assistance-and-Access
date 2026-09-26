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

    @Value("${app.embedding.api-key:${gemini.api.key:${GEMINI_API_KEY:}}}")
    private String apiKey;

    @Value("${app.embedding.api-key2:${gemini.api.key2:${GEMINI_API_KEY_2:}}}")
    private String apiKey2;

    public EmbeddingProperties() {
    }

    public EmbeddingProperties(String provider, String model, int dimension, String apiKey) {
        this(provider, model, dimension, apiKey, "");
    }

    public EmbeddingProperties(String provider, String model, int dimension, String apiKey, String apiKey2) {
        this.provider = provider;
        this.model = model;
        this.dimension = dimension;
        this.apiKey = apiKey;
        this.apiKey2 = apiKey2;
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

    public String getApiKey2() {
        return apiKey2;
    }

    public void setApiKey2(String apiKey2) {
        this.apiKey2 = apiKey2;
    }
}
