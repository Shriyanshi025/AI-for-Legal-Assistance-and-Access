package com.legalassist.service.embedding;

import java.util.List;

public interface EmbeddingService {

    List<Float> generateEmbedding(String text, EmbeddingTaskType taskType);

    List<List<Float>> generateBatchEmbeddings(List<String> texts, EmbeddingTaskType taskType);

    int getDimension();
}
