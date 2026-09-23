package com.legalassist.service;

import com.legalassist.config.EmbeddingProperties;
import com.legalassist.config.GenerationProperties;
import com.legalassist.service.embedding.EmbeddingTaskType;
import com.legalassist.service.embedding.GeminiEmbeddingServiceImpl;
import com.legalassist.service.generation.GeminiGenerationResponse;
import com.legalassist.service.generation.GeminiGenerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GeminiApiLiveTest {

    private String apiKey;
    private GeminiEmbeddingServiceImpl embeddingService;
    private GeminiGenerationServiceImpl generationService;

    @BeforeEach
    void setUp() throws Exception {
        String testProp = System.getProperty("test");
        String liveProp = System.getProperty("liveTest");
        String sunCommand = System.getProperty("sun.java.command");

        boolean isExplicitlyRequested = (testProp != null && testProp.contains("GeminiApiLiveTest"))
                || "true".equalsIgnoreCase(liveProp)
                || (sunCommand != null && sunCommand.contains("GeminiApiLiveTest"));

        assumeTrue(isExplicitlyRequested,
                "Live Gemini API test skipped during normal test suite run. Pass -Dtest=GeminiApiLiveTest or -DliveTest=true to execute.");

        apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            Path envPath = Paths.get(".env");
            if (Files.exists(envPath)) {
                Properties props = new Properties();
                try (var is = Files.newInputStream(envPath)) {
                    props.load(is);
                    apiKey = props.getProperty("GEMINI_API_KEY");
                }
            }
        }

        assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY is not configured in environment or .env file.");

        EmbeddingProperties embProps = new EmbeddingProperties("google-gemini", "gemini-embedding-001", 768, apiKey);
        embeddingService = new GeminiEmbeddingServiceImpl(embProps);

        GenerationProperties genProps = new GenerationProperties("google-gemini", "gemini-3.5-flash", 5, 0.35, 12000, 0.0, apiKey);
        generationService = new GeminiGenerationServiceImpl(genProps);
    }

    @Test
    @DisplayName("Live API test: gemini-embedding-001 generates 768-d L2 normalized vector with live key")
    void testLiveEmbedding() {
        List<Float> vector = embeddingService.generateEmbedding(
                "Legal assist contract clause test for embedding generation",
                EmbeddingTaskType.RETRIEVAL_DOCUMENT
        );

        assertThat(vector).isNotNull().hasSize(768);
        double norm = vector.stream().mapToDouble(v -> v * v).sum();
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("Live API test: gemini-2.5-flash generates grounded response with live key")
    void testLiveGeneration() {
        String systemInstruction = "You are an expert legal document assistant. Your sole task is to answer the user's question based strictly and exclusively on the provided document context below.";
        String userPrompt = "=== DOCUMENT CONTEXT ===\n[SRC-1] (Page 1, Chunk 1):\nEither party may terminate this agreement with 30 days notice.\n<<<END DOCUMENT CONTEXT>>>\n\n=== USER QUESTION ===\nWhat is the termination notice period?";

        GeminiGenerationResponse response = generationService.generateAnswer(systemInstruction, userPrompt);

        assertThat(response).isNotNull();
        assertThat(response.answer()).isNotNull().isNotEmpty();
        assertThat(response.grounded()).isTrue();
        assertThat(response.citations()).isNotEmpty();
    }
}
