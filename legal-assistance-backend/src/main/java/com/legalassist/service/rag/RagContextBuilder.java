package com.legalassist.service.rag;

import com.legalassist.dto.SimilaritySearchResultResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RagContextBuilder {

    public RagContext buildContext(List<SimilaritySearchResultResponse> chunks, double minSimilarity, int maxContextChars) {
        if (chunks == null || chunks.isEmpty()) {
            return new RagContext(List.of(), 0);
        }

        List<RagSourceItem> items = new ArrayList<>();
        int currentLength = 0;
        int sourceIndex = 1;

        for (SimilaritySearchResultResponse chunk : chunks) {
            if (chunk.similarityScore() == null || chunk.similarityScore() < minSimilarity) {
                continue;
            }

            String content = chunk.content() != null ? chunk.content() : "";
            int chunkLength = content.length();

            if (currentLength + chunkLength > maxContextChars && !items.isEmpty()) {
                // Budget reached, stop adding further chunks
                break;
            }

            String sourceId = "SRC-" + sourceIndex++;
            RagSourceItem item = new RagSourceItem(
                    sourceId,
                    chunk.chunkId(),
                    chunk.documentId(),
                    chunk.pageNumber(),
                    chunk.chunkIndex(),
                    content,
                    chunk.section(),
                    chunk.clause(),
                    chunk.similarityScore()
            );

            items.add(item);
            currentLength += chunkLength;
        }

        return new RagContext(items, currentLength);
    }
}
