package com.legalassist.controller;

import com.legalassist.dto.research.FollowUpResearchRequest;
import com.legalassist.dto.research.FollowUpResearchResponse;
import com.legalassist.dto.research.LegalResearchRequest;
import com.legalassist.dto.research.LegalResearchResponse;
import com.legalassist.dto.research.RenameResearchSessionRequest;
import com.legalassist.dto.research.ResearchSessionSummaryResponse;
import com.legalassist.security.UserPrincipal;
import com.legalassist.service.research.LegalResearchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/research")
public class ResearchController {

    private static final Logger log = LoggerFactory.getLogger(ResearchController.class);

    private final LegalResearchService legalResearchService;

    public ResearchController(LegalResearchService legalResearchService) {
        this.legalResearchService = legalResearchService;
    }

    private UUID resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new BadCredentialsException("Unauthenticated user");
    }

    @PostMapping
    public ResponseEntity<LegalResearchResponse> startResearch(
            @Valid @RequestBody LegalResearchRequest request
    ) {
        log.info("Research request received: question length={}, document count={}",
                request != null && request.researchQuestion() != null ? request.researchQuestion().length() : 0,
                request != null && request.documentIds() != null ? request.documentIds().size() : 0);
        UUID effectiveUserId = resolveUserId();
        LegalResearchResponse response = legalResearchService.executeResearch(effectiveUserId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ResearchSessionSummaryResponse>> getResearchSessions() {
        UUID effectiveUserId = resolveUserId();
        List<ResearchSessionSummaryResponse> sessions = legalResearchService.getUserResearchSessions(effectiveUserId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<LegalResearchResponse> getResearchSession(
            @PathVariable("id") UUID id
    ) {
        UUID effectiveUserId = resolveUserId();
        LegalResearchResponse response = legalResearchService.getResearchSession(id, effectiveUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{id}/followup")
    public ResponseEntity<FollowUpResearchResponse> executeFollowUp(
            @PathVariable("id") UUID id,
            @Valid @RequestBody FollowUpResearchRequest request
    ) {
        UUID effectiveUserId = resolveUserId();
        FollowUpResearchResponse response = legalResearchService.executeFollowUp(id, effectiveUserId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/sessions/{id}")
    public ResponseEntity<ResearchSessionSummaryResponse> renameResearchSession(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RenameResearchSessionRequest request
    ) {
        UUID effectiveUserId = resolveUserId();
        ResearchSessionSummaryResponse response = legalResearchService.renameResearchSession(id, effectiveUserId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteResearchSession(
            @PathVariable("id") UUID id
    ) {
        UUID effectiveUserId = resolveUserId();
        legalResearchService.deleteResearchSession(id, effectiveUserId);
        return ResponseEntity.noContent().build();
    }
}
