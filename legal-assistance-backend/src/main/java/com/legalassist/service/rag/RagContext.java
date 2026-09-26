package com.legalassist.service.rag;

import java.util.List;

public record RagContext(
        List<RagSourceItem> items,
        int totalCharacters
) {
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    /**
     * Formats the RAG context items into a readable text block.
     *
     * @return Formatted context string
     */
    public String formattedContext() {
        if (items == null || items.isEmpty()) {
            return "No relevant document evidence found.";
        }
        StringBuilder sb = new StringBuilder();
        for (RagSourceItem item : items) {
            sb.append("[").append(item.sourceId()).append("] ");
            if (item.pageNumber() != null) {
                sb.append("(Page ").append(item.pageNumber());
                if (item.chunkIndex() != null) {
                    sb.append(", Chunk ").append(item.chunkIndex());
                }
                sb.append(")");
            }
            sb.append(":\n");
            sb.append(item.content()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
