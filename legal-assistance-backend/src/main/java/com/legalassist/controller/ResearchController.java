package com.legalassist.controller;

import com.legalassist.dto.research.LegalResearchRequest;
import com.legalassist.dto.research.LegalResearchResponse;
import com.legalassist.dto.research.ResearchSessionSummaryResponse;
import com.legalassist.service.UserService;
import com.legalassist.service.research.LegalResearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/research")
public class ResearchController {

    private static final Logger log = LoggerFactory.getLogger(ResearchController.class);

    private final LegalResearchService legalResearchService;
    private final UserService userService;

    public ResearchController(LegalResearchService legalResearchService, UserService userService) {
        this.legalResearchService = legalResearchService;
        this.userService = userService;
    }

    private UUID resolveUserId(UUID queryUserId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.legalassist.security.UserPrincipal principal) {
            return principal.getId();
        }
        return userService.getOrCreateUser(queryUserId).getId();
    }

    @PostMapping
    public ResponseEntity<LegalResearchResponse> startResearch(
            @RequestParam(value = "userId", required = false) UUID userId,
            @Valid @RequestBody LegalResearchRequest request
    ) {
        log.info("Research request received: question length={}, document count={}",
                request != null && request.researchQuestion() != null ? request.researchQuestion().length() : 0,
                request != null && request.documentIds() != null ? request.documentIds().size() : 0);
        UUID effectiveUserId = resolveUserId(userId);
        LegalResearchResponse response = legalResearchService.executeResearch(effectiveUserId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ResearchSessionSummaryResponse>> getResearchSessions(
            @RequestParam(value = "userId", required = false) UUID userId
    ) {
        UUID effectiveUserId = resolveUserId(userId);
        List<ResearchSessionSummaryResponse> sessions = legalResearchService.getUserResearchSessions(effectiveUserId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<LegalResearchResponse> getResearchSession(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId
    ) {
        UUID effectiveUserId = resolveUserId(userId);
        LegalResearchResponse response = legalResearchService.getResearchSession(id, effectiveUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{id}/followup")
    public ResponseEntity<com.legalassist.dto.research.FollowUpResearchResponse> executeFollowUp(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId,
            @Valid @RequestBody com.legalassist.dto.research.FollowUpResearchRequest request
    ) {
        UUID effectiveUserId = resolveUserId(userId);
        com.legalassist.dto.research.FollowUpResearchResponse response = legalResearchService.executeFollowUp(id, effectiveUserId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/sessions/{id}")
    public ResponseEntity<ResearchSessionSummaryResponse> renameResearchSession(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId,
            @Valid @RequestBody com.legalassist.dto.research.RenameResearchSessionRequest request
    ) {
        UUID effectiveUserId = resolveUserId(userId);
        ResearchSessionSummaryResponse response = legalResearchService.renameResearchSession(id, effectiveUserId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteResearchSession(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId
    ) {
        UUID effectiveUserId = resolveUserId(userId);
        legalResearchService.deleteResearchSession(id, effectiveUserId);
        return ResponseEntity.noContent().build();
    }
}
