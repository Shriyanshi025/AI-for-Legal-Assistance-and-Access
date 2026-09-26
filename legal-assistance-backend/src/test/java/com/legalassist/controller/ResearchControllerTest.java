package com.legalassist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalassist.dto.research.LegalResearchRequest;
import com.legalassist.dto.research.LegalResearchResponse;
import com.legalassist.entity.User;
import com.legalassist.security.UserPrincipal;
import com.legalassist.service.UserService;
import com.legalassist.service.research.LegalResearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ResearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private LegalResearchService legalResearchService;

    @MockitoBean
    private UserService userService;

    private UUID userId;
    private UUID docId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        docId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setPublicUserId("USR-TEST12");
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed_password");
        user.setEnabled(true);

        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/research should return 200 OK with research response")
    void startResearchSuccess() throws Exception {
        LegalResearchRequest request = new LegalResearchRequest("Notice period requirement?", List.of(docId), "Comprehensive", "US", null);
        LegalResearchResponse mockResponse = new LegalResearchResponse(
                UUID.randomUUID(),
                "Notice period requirement?",
                "Comprehensive",
                "US",
                null,
                List.of(docId),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                LocalDateTime.now()
        );

        when(legalResearchService.executeResearch(eq(userId), any(LegalResearchRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/research")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.researchQuestion").value("Notice period requirement?"))
                .andExpect(jsonPath("$.researchType").value("Comprehensive"));
    }

    @Test
    @DisplayName("GET /api/research/sessions should return list of sessions")
    void getResearchSessionsSuccess() throws Exception {
        when(legalResearchService.getUserResearchSessions(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/research/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/research/sessions/{id}/followup should execute follow-up research")
    void executeFollowUpSuccess() throws Exception {
        UUID sessionId = UUID.randomUUID();
        com.legalassist.dto.research.FollowUpResearchRequest request = new com.legalassist.dto.research.FollowUpResearchRequest("What is the penalty?");
        com.legalassist.dto.research.FollowUpResearchResponse response = new com.legalassist.dto.research.FollowUpResearchResponse(
                sessionId,
                "What is the penalty?",
                "The penalty is 5% late fee.",
                List.of(),
                LocalDateTime.now()
        );

        when(legalResearchService.executeFollowUp(eq(sessionId), eq(userId), any())).thenReturn(response);

        mockMvc.perform(post("/api/research/sessions/" + sessionId + "/followup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("The penalty is 5% late fee."));
    }

    @Test
    @DisplayName("PATCH /api/research/sessions/{id} should rename session")
    void renameResearchSessionSuccess() throws Exception {
        UUID sessionId = UUID.randomUUID();
        com.legalassist.dto.research.RenameResearchSessionRequest request = new com.legalassist.dto.research.RenameResearchSessionRequest("New Title");
        com.legalassist.dto.research.ResearchSessionSummaryResponse response = new com.legalassist.dto.research.ResearchSessionSummaryResponse(
                sessionId,
                "New Title",
                "Comprehensive",
                1, 2, 0, 0,
                LocalDateTime.now()
        );

        when(legalResearchService.renameResearchSession(eq(sessionId), eq(userId), any())).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/research/sessions/" + sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.researchQuestion").value("New Title"));
    }

    @Test
    @DisplayName("DELETE /api/research/sessions/{id} should delete session")
    void deleteResearchSessionSuccess() throws Exception {
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/research/sessions/" + sessionId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/research should return 401 Unauthorized")
    void unauthenticatedResearchFails() throws Exception {
        SecurityContextHolder.clearContext();
        LegalResearchRequest request = new LegalResearchRequest("Notice period requirement?", List.of(docId), "Comprehensive", "US", null);

        mockMvc.perform(post("/api/research")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
