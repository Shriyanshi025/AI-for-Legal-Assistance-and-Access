package com.legalassist.service.embedding;

import com.legalassist.config.EmbeddingProperties;
import com.legalassist.exception.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiEmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingServiceImpl.class);

    private final EmbeddingProperties embeddingProperties;
    private final RestClient restClient;

    public GeminiEmbeddingServiceImpl(EmbeddingProperties embeddingProperties) {
        this(embeddingProperties, RestClient.create());
    }

    public GeminiEmbeddingServiceImpl(EmbeddingProperties embeddingProperties, RestClient restClient) {
        this.embeddingProperties = embeddingProperties;
        this.restClient = restClient;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiEmbeddingServiceImpl(EmbeddingProperties embeddingProperties, org.springframework.beans.factory.ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
        this(embeddingProperties, restClientBuilderProvider.getIfAvailable(RestClient::builder).build());
    }

    @Override
    public List<Float> generateEmbedding(String text, EmbeddingTaskType taskType) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text content for embedding generation cannot be null or blank");
        }
        if (taskType == null) {
            throw new IllegalArgumentException("EmbeddingTaskType cannot be null");
        }

        String primaryKey = embeddingProperties.getApiKey();
        if (primaryKey == null || primaryKey.isBlank()) {
            throw new EmbeddingException("Gemini API key is missing. Set GEMINI_API_KEY environment variable.");
        }

        List<String> keysToTry = new ArrayList<>();
        keysToTry.add(primaryKey);
        String secondaryKey = embeddingProperties.getApiKey2();
        if (secondaryKey != null && !secondaryKey.isBlank() && !secondaryKey.equals(primaryKey)) {
            keysToTry.add(secondaryKey);
        }

        String modelName = embeddingProperties.getModel();
        int expectedDimension = embeddingProperties.getDimension();

        Map<String, Object> requestPayload = Map.of(
                "model", "models/" + modelName,
                "content", Map.of(
                        "parts", List.of(Map.of("text", text))
                ),
                "taskType", taskType.getValue(),
                "outputDimensionality", expectedDimension
        );

        Map<?, ?> response = null;
        Throwable lastException = null;

        for (int keyIdx = 0; keyIdx < keysToTry.size(); keyIdx++) {
            String apiKey = keysToTry.get(keyIdx);
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s",
                    modelName, apiKey);
            try {
                response = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestPayload)
                        .retrieve()
                        .body(Map.class);
                break;
            } catch (Exception e) {
                lastException = e;
                if (keyIdx < keysToTry.size() - 1 && isTemporaryError(e)) {
                    log.warn("Gemini embedding key #{} encountered quota/transient error. Trying secondary key...", keyIdx + 1);
                    continue;
                }
                log.error("Failed Gemini API embedding request for text length {}", text.length(), e);
                throw new EmbeddingException("Failed to generate embedding from Gemini API: " + e.getMessage(), e);
            }
        }

        if (response == null) {
            throw new EmbeddingException("Failed to generate embedding from Gemini API across configured keys", lastException);
        }

        List<Float> rawValues = extractEmbeddingValues(response, expectedDimension);
        return VectorUtils.l2Normalize(rawValues);
    }

    @Override
    public List<List<Float>> generateBatchEmbeddings(List<String> texts, EmbeddingTaskType taskType) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<List<Float>> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(generateEmbedding(text, taskType));
        }
        return result;
    }

    @Override
    public int getDimension() {
        return embeddingProperties.getDimension();
    }

    @SuppressWarnings("unchecked")
    private List<Float> extractEmbeddingValues(Map<?, ?> response, int expectedDimension) {
        if (response == null || !response.containsKey("embedding")) {
            throw new EmbeddingException("Invalid Gemini API response: missing 'embedding' field");
        }

        Object embeddingObj = response.get("embedding");
        if (!(embeddingObj instanceof Map<?, ?> embeddingMap) || !embeddingMap.containsKey("values")) {
            throw new EmbeddingException("Invalid Gemini API response: missing 'values' in embedding payload");
        }

        Object valuesObj = embeddingMap.get("values");
        if (!(valuesObj instanceof List<?> valuesList)) {
            throw new EmbeddingException("Invalid Gemini API response: 'values' is not a list");
        }

        if (valuesList.size() != expectedDimension) {
            throw new EmbeddingException(String.format(
                    "Unexpected embedding dimension from Gemini API: expected %d but received %d",
                    expectedDimension, valuesList.size()
            ));
        }

        List<Float> floatValues = new ArrayList<>(valuesList.size());
        for (Object val : valuesList) {
            if (val instanceof Number num) {
                floatValues.add(num.floatValue());
            } else {
                throw new EmbeddingException("Embedding element is not a numeric float value: " + val);
            }
        }

        return floatValues;
    }

    private boolean isTemporaryError(Throwable t) {
        if (t == null) return false;
        Throwable curr = t;
        while (curr != null) {
            String msg = curr.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("429") || lower.contains("503") || lower.contains("resource_exhausted") || lower.contains("quota")) {
                    return true;
                }
            }
            curr = curr.getCause();
        }
        return false;
    }
}
