package com.legalassist.service.research;

import com.legalassist.dto.research.FollowUpResearchRequest;
import com.legalassist.dto.research.FollowUpResearchResponse;
import com.legalassist.dto.research.LegalResearchRequest;
import com.legalassist.dto.research.LegalResearchResponse;
import com.legalassist.dto.research.RenameResearchSessionRequest;
import com.legalassist.dto.research.ResearchSessionSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface LegalResearchService {

    LegalResearchResponse executeResearch(UUID userId, LegalResearchRequest request);

    List<ResearchSessionSummaryResponse> getUserResearchSessions(UUID userId);

    LegalResearchResponse getResearchSession(UUID sessionId, UUID userId);

    FollowUpResearchResponse executeFollowUp(UUID sessionId, UUID userId, FollowUpResearchRequest request);

    ResearchSessionSummaryResponse renameResearchSession(UUID sessionId, UUID userId, RenameResearchSessionRequest request);

    void deleteResearchSession(UUID sessionId, UUID userId);
}
