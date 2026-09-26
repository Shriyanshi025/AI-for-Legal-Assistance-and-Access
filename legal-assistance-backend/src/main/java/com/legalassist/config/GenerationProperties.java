package com.legalassist.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rag")
public class GenerationProperties {

    private String generationProvider = "google-gemini";
    private String generationModel = "gemini-3.8-flash";
    private String fallbackModel = "";
    private int topK = 5;

    private double minSimilarity = 0.35;
    private int maxContextChars = 12000;
    private double temperature = 0.0;

    @Value("${app.rag.api-key:${gemini.api.key:${GEMINI_API_KEY:}}}")
    private String apiKey;

    @Value("${app.rag.api-key2:${gemini.api.key2:${GEMINI_API_KEY_2:}}}")
    private String apiKey2;

    public GenerationProperties() {
    }

    public GenerationProperties(String generationProvider, String generationModel, int topK, double minSimilarity, int maxContextChars, double temperature, String apiKey) {
        this(generationProvider, generationModel, "", topK, minSimilarity, maxContextChars, temperature, apiKey, "");
    }

    public GenerationProperties(String generationProvider, String generationModel, String fallbackModel, int topK, double minSimilarity, int maxContextChars, double temperature, String apiKey) {
        this(generationProvider, generationModel, fallbackModel, topK, minSimilarity, maxContextChars, temperature, apiKey, "");
    }

    public GenerationProperties(String generationProvider, String generationModel, String fallbackModel, int topK, double minSimilarity, int maxContextChars, double temperature, String apiKey, String apiKey2) {
        this.generationProvider = generationProvider;
        this.generationModel = generationModel;
        this.fallbackModel = fallbackModel;
        this.topK = topK;
        this.minSimilarity = minSimilarity;
        this.maxContextChars = maxContextChars;
        this.temperature = temperature;
        this.apiKey = apiKey;
        this.apiKey2 = apiKey2;
    }

    public String getFallbackModel() {
        return fallbackModel;
    }

    public void setFallbackModel(String fallbackModel) {
        this.fallbackModel = fallbackModel;
    }

    public String getGenerationProvider() {
        return generationProvider;
    }

    public void setGenerationProvider(String generationProvider) {
        this.generationProvider = generationProvider;
    }

    public String getGenerationModel() {
        return generationModel;
    }

    public void setGenerationModel(String generationModel) {
        this.generationModel = generationModel;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinSimilarity() {
        return minSimilarity;
    }

    public void setMinSimilarity(double minSimilarity) {
        this.minSimilarity = minSimilarity;
    }

    public int getMaxContextChars() {
        return maxContextChars;
    }

    public void setMaxContextChars(int maxContextChars) {
        this.maxContextChars = maxContextChars;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
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
