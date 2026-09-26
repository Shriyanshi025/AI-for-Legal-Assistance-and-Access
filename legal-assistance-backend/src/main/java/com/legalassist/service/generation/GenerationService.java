package com.legalassist.service.generation;

public interface GenerationService {
    GeminiGenerationResponse generateAnswer(String systemInstruction, String userPrompt);
    String generateRawContent(String systemInstruction, String userPrompt);
}
