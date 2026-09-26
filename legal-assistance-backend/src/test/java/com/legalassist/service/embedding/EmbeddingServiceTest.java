package com.legalassist.service.embedding;

import com.legalassist.config.EmbeddingProperties;
import com.legalassist.exception.EmbeddingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class EmbeddingServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private EmbeddingProperties embeddingProperties;
    private GeminiEmbeddingServiceImpl embeddingService;

    @BeforeEach
    void setUp() {
        embeddingProperties = new EmbeddingProperties("google-gemini", "gemini-embedding-001", 768, "dummy-test-key");
        embeddingService = new GeminiEmbeddingServiceImpl(embeddingProperties, restClient);
    }

    @Test
    @DisplayName("generateEmbedding should issue Gemini request and return L2-normalized 768-d vector")
    void generateEmbeddingSuccess() {
        List<Float> raw768Values = new ArrayList<>(Collections.nCopies(768, 1.0f));
        Map<String, Object> mockResponse = Map.of(
                "embedding", Map.of("values", raw768Values)
        );

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(String.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(mockResponse).when(responseSpec).body(eq(Map.class));

        List<Float> result = embeddingService.generateEmbedding("Legal clause text", EmbeddingTaskType.RETRIEVAL_DOCUMENT);

        assertThat(result).hasSize(768);
        double norm = 0.0;
        for (Float f : result) {
            norm += f * f;
        }
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    @DisplayName("generateEmbedding should throw IllegalArgumentException when text is null or blank")
    void generateEmbeddingShouldRejectNullOrBlank() {
        assertThatThrownBy(() -> embeddingService.generateEmbedding(null, EmbeddingTaskType.RETRIEVAL_DOCUMENT))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> embeddingService.generateEmbedding("   ", EmbeddingTaskType.RETRIEVAL_DOCUMENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("generateEmbedding should throw IllegalArgumentException when taskType is null")
    void generateEmbeddingShouldRejectNullTaskType() {
        assertThatThrownBy(() -> embeddingService.generateEmbedding("text", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("generateEmbedding should throw EmbeddingException when API key is missing")
    void generateEmbeddingShouldThrowWhenApiKeyMissing() {
        EmbeddingProperties noKeyProperties = new EmbeddingProperties("google-gemini", "gemini-embedding-001", 768, "");
        GeminiEmbeddingServiceImpl service = new GeminiEmbeddingServiceImpl(noKeyProperties, restClient);

        assertThatThrownBy(() -> service.generateEmbedding("text", EmbeddingTaskType.RETRIEVAL_QUERY))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("generateEmbedding should throw EmbeddingException when API returns unexpected dimension size")
    void generateEmbeddingShouldThrowWhenDimensionMismatch() {
        List<Float> shortVector = List.of(0.1f, 0.2f, 0.3f);
        Map<String, Object> mockResponse = Map.of(
                "embedding", Map.of("values", shortVector)
        );

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(String.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(mockResponse).when(responseSpec).body(eq(Map.class));

        assertThatThrownBy(() -> embeddingService.generateEmbedding("text", EmbeddingTaskType.RETRIEVAL_QUERY))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("768");
    }
}
