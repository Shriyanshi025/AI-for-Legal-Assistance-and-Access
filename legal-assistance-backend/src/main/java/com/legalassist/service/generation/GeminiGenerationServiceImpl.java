package com.legalassist.service.generation;

import com.legalassist.config.GenerationProperties;
import com.legalassist.exception.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiGenerationServiceImpl implements GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GeminiGenerationServiceImpl.class);

    private final GenerationProperties generationProperties;
    private final RestClient restClient;

    public GeminiGenerationServiceImpl(GenerationProperties generationProperties) {
        this(generationProperties, RestClient.create());
    }

    public GeminiGenerationServiceImpl(GenerationProperties generationProperties, RestClient restClient) {
        this.generationProperties = generationProperties;
        this.restClient = restClient;
    }

    @Override
    public GeminiGenerationResponse generateAnswer(String systemInstruction, String userPrompt) {
        if (systemInstruction == null || systemInstruction.isBlank()) {
            throw new IllegalArgumentException("System instruction cannot be null or blank");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("User prompt cannot be null or blank");
        }

        String apiKey = generationProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new GenerationException("Gemini API key is missing. Set GEMINI_API_KEY environment variable.");
        }

        String modelName = generationProperties.getGenerationModel();
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                modelName, apiKey);

        Map<String, Object> requestPayload = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ),
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", userPrompt))
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", generationProperties.getTemperature(),
                        "responseMimeType", "application/json",
                        "responseSchema", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "answer", Map.of("type", "STRING"),
                                        "grounded", Map.of("type", "BOOLEAN"),
                                        "citations", Map.of(
                                                "type", "ARRAY",
                                                "items", Map.of("type", "STRING")
                                        )
                                ),
                                "required", List.of("answer", "grounded", "citations")
                        )
                )
        );

        Map<?, ?> response;
        try {
            response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.error("Failed Gemini API generation request for prompt length {}", userPrompt.length(), e);
            throw new GenerationException("Failed to generate answer from Gemini API: " + e.getMessage(), e);
        }

        String jsonText = extractResponseText(response);
        return parseGenerationResponse(jsonText);
    }

    @SuppressWarnings("unchecked")
    private GeminiGenerationResponse parseGenerationResponse(String jsonText) {
        try {
            JsonParser parser = JsonParserFactory.getJsonParser();
            Map<String, Object> map = parser.parseMap(jsonText);

            String answer = map.get("answer") != null ? map.get("answer").toString() : null;
            boolean grounded = Boolean.TRUE.equals(map.get("grounded")) || "true".equalsIgnoreCase(String.valueOf(map.get("grounded")));

            List<String> citations = new ArrayList<>();
            Object citationsObj = map.get("citations");
            if (citationsObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null) {
                        citations.add(item.toString());
                    }
                }
            }

            return new GeminiGenerationResponse(answer, grounded, citations);
        } catch (Exception e) {
            log.error("Failed to parse Gemini structured JSON response: {}", jsonText, e);
            throw new GenerationException("Failed to parse structured JSON response from Gemini API: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractResponseText(Map<?, ?> response) {
        if (response == null || !response.containsKey("candidates")) {
            throw new GenerationException("Invalid Gemini API response: missing 'candidates' field");
        }

        Object candidatesObj = response.get("candidates");
        if (!(candidatesObj instanceof List<?> candidatesList) || candidatesList.isEmpty()) {
            throw new GenerationException("Invalid Gemini API response: empty 'candidates' list");
        }

        Object firstCandidate = candidatesList.get(0);
        if (!(firstCandidate instanceof Map<?, ?> candidateMap) || !candidateMap.containsKey("content")) {
            throw new GenerationException("Invalid Gemini API response: missing 'content' in candidate");
        }

        Object contentObj = candidateMap.get("content");
        if (!(contentObj instanceof Map<?, ?> contentMap) || !contentMap.containsKey("parts")) {
            throw new GenerationException("Invalid Gemini API response: missing 'parts' in content");
        }

        Object partsObj = contentMap.get("parts");
        if (!(partsObj instanceof List<?> partsList) || partsList.isEmpty()) {
            throw new GenerationException("Invalid Gemini API response: empty 'parts' list");
        }

        Object firstPart = partsList.get(0);
        if (!(firstPart instanceof Map<?, ?> partMap) || !partMap.containsKey("text")) {
            throw new GenerationException("Invalid Gemini API response: missing 'text' in part");
        }

        Object textObj = partMap.get("text");
        if (!(textObj instanceof String textStr) || textStr.isBlank()) {
            throw new GenerationException("Invalid Gemini API response: empty 'text' field");
        }

        return textStr;
    }
}
