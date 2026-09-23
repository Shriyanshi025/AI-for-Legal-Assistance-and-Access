package com.legalassist.service.embedding;

public enum EmbeddingTaskType {
    RETRIEVAL_DOCUMENT("RETRIEVAL_DOCUMENT"),
    RETRIEVAL_QUERY("RETRIEVAL_QUERY");

    private final String value;

    EmbeddingTaskType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
