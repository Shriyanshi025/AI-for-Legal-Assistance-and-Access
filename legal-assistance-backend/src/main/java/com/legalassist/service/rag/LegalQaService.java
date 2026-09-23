package com.legalassist.service.rag;

import com.legalassist.dto.LegalAskRequest;
import com.legalassist.dto.LegalAnswerResponse;

import java.util.UUID;

public interface LegalQaService {
    LegalAnswerResponse askQuestion(UUID documentId, LegalAskRequest request);
}
