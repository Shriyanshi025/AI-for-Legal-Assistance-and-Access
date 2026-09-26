package com.legalassist.service;

import com.legalassist.config.GenerationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationPropertiesKeyLoadingTest {

    @Test
    @DisplayName("Verify GenerationProperties supports apiKey and apiKey2 configuration")
    void testKeyProperties() {
        GenerationProperties props = new GenerationProperties("google-gemini", "gemini-3.8-flash", "", 5, 0.35, 12000, 0.0, "key1", "key2");
        assertThat(props.getApiKey()).isEqualTo("key1");
        assertThat(props.getApiKey2()).isEqualTo("key2");
    }
}
