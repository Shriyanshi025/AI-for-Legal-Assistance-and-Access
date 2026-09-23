package com.legalassist.service.rag;

import java.util.List;

public record RagContext(
        List<RagSourceItem> items,
        int totalCharacters
) {
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
