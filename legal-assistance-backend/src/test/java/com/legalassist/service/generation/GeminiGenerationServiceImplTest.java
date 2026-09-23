package com.legalassist.service.generation;

import com.legalassist.config.GenerationProperties;
import com.legalassist.exception.GenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class GeminiGenerationServiceImplTest {

    @Mock
    private org.springframework.web.client.RestClient restClient;

    @Mock
    private org.springframework.web.client.RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private org.springframework.web.client.RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private org.springframework.web.client.RestClient.ResponseSpec responseSpec;

    private GenerationProperties generationProperties;
    private GeminiGenerationServiceImpl generationService;

    @BeforeEach
    void setUp() {
        generationProperties = new GenerationProperties("google-gemini", "gemini-3.5-flash", 5, 0.35, 12000, 0.0, "test-api-key");
        generationService = new GeminiGenerationServiceImpl(generationProperties, restClient);
    }

    @Test
    @DisplayName("generateAnswer should issue Gemini request with correct structured-output JSON payload schema")
    @SuppressWarnings("unchecked")
    void generateAnswerVerifyPayloadStructureAndSuccess() {
        String mockModelJson = """
                {"answer": "Either party may terminate on 30 days notice.", "grounded": true, "citations": ["SRC-1"]}
                """;

        Map<String, Object> mockGeminiResponse = Map.of(
                "candidates", List.of(
                        Map.of(
                                "content", Map.of(
                                        "parts", List.of(
                                                Map.of("text", mockModelJson)
                                        )
                                )
                        )
                )
        );

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(String.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(mockGeminiResponse).when(responseSpec).body(eq(Map.class));

        GeminiGenerationResponse response = generationService.generateAnswer(
                "System instructions text",
                "User prompt text"
        );

        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo("Either party may terminate on 30 days notice.");
        assertThat(response.grounded()).isTrue();
        assertThat(response.citations()).containsExactly("SRC-1");

        // Verify request payload structure captured by RestClient
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodySpec).body(bodyCaptor.capture());

        assertThat(bodyCaptor.getValue()).isInstanceOf(Map.class);
        Map<String, Object> payload = (Map<String, Object>) bodyCaptor.getValue();

        assertThat(payload).containsKey("systemInstruction");
        assertThat(payload).containsKey("contents");
        assertThat(payload).containsKey("generationConfig");

        Map<String, Object> genConfig = (Map<String, Object>) payload.get("generationConfig");
        assertThat(genConfig.get("temperature")).isEqualTo(0.0);
        assertThat(genConfig.get("responseMimeType")).isEqualTo("application/json");

        Map<String, Object> schema = (Map<String, Object>) genConfig.get("responseSchema");
        assertThat(schema.get("type")).isEqualTo("OBJECT");
        assertThat((List<String>) schema.get("required")).containsExactly("answer", "grounded", "citations");
    }

    @Test
    @DisplayName("generateAnswer should throw GenerationException when API key is missing")
    void generateAnswerShouldThrowWhenApiKeyMissing() {
        GenerationProperties noKeyProps = new GenerationProperties("google-gemini", "gemini-2.5-flash", 5, 0.35, 12000, 0.0, "");
        GeminiGenerationServiceImpl service = new GeminiGenerationServiceImpl(noKeyProps, restClient);

        assertThatThrownBy(() -> service.generateAnswer("Sys", "User"))
                .isInstanceOf(GenerationException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("generateAnswer should throw GenerationException on API call failure")
    void generateAnswerShouldThrowOnApiFailure() {
        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(String.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        when(responseSpec.body(eq(Map.class))).thenThrow(new RuntimeException("Network error"));

        assertThatThrownBy(() -> generationService.generateAnswer("Sys", "User"))
                .isInstanceOf(GenerationException.class)
                .hasMessageContaining("Failed to generate answer");
    }
}
