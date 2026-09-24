package com.legalassist.service.rag;

import com.legalassist.dto.CitationResponse;
import com.legalassist.dto.LegalAnswerResponse;
import com.legalassist.exception.GenerationException;
import com.legalassist.service.generation.GeminiGenerationResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CitationValidator {

    public static final String INSUFFICIENT_CONTEXT_ANSWER = 
            "The provided document does not contain enough information to answer this question.";

    public LegalAnswerResponse validateAndBuildResponse(GeminiGenerationResponse rawResponse, RagContext context) {
        if (rawResponse == null) {
            throw new GenerationException("Generation response cannot be null");
        }

        String answer = rawResponse.answer();
        if (answer == null || answer.isBlank()) {
            throw new GenerationException("Gemini API returned an empty or null answer");
        }

        Map<String, RagSourceItem> sourceMap = context.items().stream()
                .collect(Collectors.toMap(RagSourceItem::sourceId, Function.identity()));

        List<RagSourceItem> matchedItems = new ArrayList<>();
        if (rawResponse.citations() != null) {
            for (String rawCitationId : rawResponse.citations()) {
                if (rawCitationId == null || rawCitationId.isBlank()) {
                    continue;
                }
                String cleanId = rawCitationId.trim();
                if (cleanId.startsWith("[") && cleanId.endsWith("]")) {
                    cleanId = cleanId.substring(1, cleanId.length() - 1).trim();
                }
                RagSourceItem item = sourceMap.get(cleanId);
                if (item != null && !matchedItems.contains(item)) {
                    matchedItems.add(item);
                }
            }
        }



        boolean grounded = rawResponse.grounded();

        if (grounded && matchedItems.isEmpty()) {
            grounded = false;
        }

        if (!grounded) {
            return new LegalAnswerResponse(INSUFFICIENT_CONTEXT_ANSWER, false, List.of());
        }

        List<CitationResponse> citations = matchedItems.stream()
                .map(this::toCitationResponse)
                .filter(Objects::nonNull)
                .toList();

        return new LegalAnswerResponse(answer, true, citations);
    }

    private CitationResponse toCitationResponse(RagSourceItem item) {
        String excerpt = item.content();
        if (excerpt != null && excerpt.length() > 200) {
            excerpt = excerpt.substring(0, 197) + "...";
        }
        return new CitationResponse(item.documentId(), item.pageNumber(), item.chunkIndex(), excerpt);
    }
}
