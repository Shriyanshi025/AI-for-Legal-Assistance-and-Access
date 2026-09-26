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
        generationProperties = new GenerationProperties("google-gemini", "gemini-3.8-flash", "", 5, 0.35, 12000, 0.0, "test-api-key");
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
    @DisplayName("Test 1: Gemini succeeds on first attempt")
    @SuppressWarnings("unchecked")
    void test1_SucceedsOnFirstAttempt() {
        String mockModelJson = """
                {"answer": "Termination notice period is 30 days.", "grounded": true, "citations": ["SRC-1", "SRC-2"]}
                """;
        Map<String, Object> mockResponse = Map.of(
                "candidates", List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", mockModelJson)))))
        );

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(String.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(mockResponse).when(responseSpec).body(eq(Map.class));

        GeminiGenerationResponse response = generationService.generateAnswer("System instruction", "User prompt");

        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo("Termination notice period is 30 days.");
        assertThat(response.grounded()).isTrue();
        assertThat(response.citations()).containsExactly("SRC-1", "SRC-2");
    }

    @Test
    @DisplayName("Test 2: First attempt 503, second attempt succeeds (retry occurs)")
    @SuppressWarnings("unchecked")
    void test2_RetryOn503ThenSucceeds() {
        String mockModelJson = """
                {"answer": "Grounded answer after 503 retry.", "grounded": true, "citations": ["SRC-1"]}
                """;
        Map<String, Object> mockSuccessResponse = Map.of(
                "candidates", List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", mockModelJson)))))
        );

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(String.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();

        when(responseSpec.body(eq(Map.class)))
                .thenThrow(new RuntimeException("503 Service Unavailable: High demand"))
                .thenReturn(mockSuccessResponse);

        GeminiGenerationResponse response = generationService.generateAnswer("Sys", "User");

        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo("Grounded answer after 503 retry.");
        assertThat(response.citations()).containsExactly("SRC-1");
    }

    @Test
    @DisplayName("Test 3: Both retry attempts on Key 1 fail with 503, throws AiServiceUnavailableException")
    @SuppressWarnings("unchecked")
    void test3_RetryOn503Exhausted_ThrowsAiServiceUnavailableException() {
        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(String.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();

        when(responseSpec.body(eq(Map.class)))
                .thenThrow(new RuntimeException("503 Service Unavailable"))
                .thenThrow(new RuntimeException("503 Service Unavailable"));

        assertThatThrownBy(() -> generationService.generateAnswer("Sys", "User"))
                .isInstanceOf(com.legalassist.exception.AiServiceUnavailableException.class)
                .hasMessageContaining("AI service is temporarily unavailable");
    }

    @Test
    @DisplayName("Test 4: 429 Quota Exhaustion on Key 1 rotates to Key 2 and succeeds")
    @SuppressWarnings("unchecked")
    void test4_QuotaExhaustionKey1_RotatesToKey2_Succeeds() {
        GenerationProperties twoKeyProps = new GenerationProperties("google-gemini", "gemini-3.8-flash", "", 5, 0.35, 12000, 0.0, "key1", "key2");
        GeminiGenerationServiceImpl service = new GeminiGenerationServiceImpl(twoKeyProps, restClient);

        String mockModelJson = """
                {"answer": "Answer generated using key2 fallback.", "grounded": true, "citations": ["SRC-KEY2"]}
                """;
        Map<String, Object> mockSuccessResponse = Map.of(
                "candidates", List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", mockModelJson)))))
        );

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(String.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();

        when(responseSpec.body(eq(Map.class)))
                .thenThrow(new RuntimeException("429 RESOURCE_EXHAUSTED: GenerateRequestsPerDayPerProject-FreeTier"))
                .thenReturn(mockSuccessResponse);

        GeminiGenerationResponse response = service.generateAnswer("Sys", "User");

        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo("Answer generated using key2 fallback.");
        assertThat(response.citations()).containsExactly("SRC-KEY2");
    }

    @Test
    @DisplayName("Test 5: Quota Exhaustion on all keys triggers immediate fail-fast without long retries")
    @SuppressWarnings("unchecked")
    void test5_QuotaExhaustionAllKeys_FailsFast() {
        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(String.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();

        when(responseSpec.body(eq(Map.class)))
                .thenThrow(new RuntimeException("429 RESOURCE_EXHAUSTED: GenerateRequestsPerDayPerProject-FreeTier"));

        assertThatThrownBy(() -> generationService.generateAnswer("Sys", "User"))
                .isInstanceOf(com.legalassist.exception.AiServiceUnavailableException.class)
                .hasMessageContaining("AI service quota is temporarily unavailable");
    }

    @Test
    @DisplayName("Test 6: Non-retryable error (e.g. 400 Bad Request) fails immediately without retrying")
    @SuppressWarnings("unchecked")
    void test6_NonRetryableError_FailsImmediately() {
        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(String.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();

        when(responseSpec.body(eq(Map.class)))
                .thenThrow(new RuntimeException("400 Bad Request: Invalid JSON body"));

        assertThatThrownBy(() -> generationService.generateAnswer("Sys", "User"))
                .isInstanceOf(GenerationException.class)
                .isNotInstanceOf(com.legalassist.exception.AiServiceUnavailableException.class)
                .hasMessageContaining("Failed to generate answer");
    }

    @Test
    @DisplayName("Test 7: Citation data remains unchanged when generation succeeds")
    @SuppressWarnings("unchecked")
    void test7_CitationDataRemainsUnchanged() {
        String mockModelJson = """
                {"answer": "Grounded answer text.", "grounded": true, "citations": ["SRC-1", "SRC-2", "SRC-3"]}
                """;
        Map<String, Object> mockResponse = Map.of(
                "candidates", List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", mockModelJson)))))
        );

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(String.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(mockResponse).when(responseSpec).body(eq(Map.class));

        GeminiGenerationResponse response = generationService.generateAnswer("Sys", "User");

        assertThat(response.citations()).hasSize(3);
        assertThat(response.citations()).containsExactly("SRC-1", "SRC-2", "SRC-3");
    }
}
