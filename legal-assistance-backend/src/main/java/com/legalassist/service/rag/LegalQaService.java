package com.legalassist.service.rag;

import com.legalassist.dto.LegalAskRequest;
import com.legalassist.dto.LegalAnswerResponse;

import java.util.List;
import java.util.UUID;

public interface LegalQaService {
    LegalAnswerResponse askQuestion(UUID documentId, LegalAskRequest request);
    LegalAnswerResponse askMultiDocumentQuestion(List<UUID> documentIds, LegalAskRequest request);
}
