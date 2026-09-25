package com.legalassist.controller;

import com.legalassist.dto.CitationResponse;
import com.legalassist.dto.LegalAnswerResponse;
import com.legalassist.exception.DocumentNotFoundException;
import com.legalassist.exception.GlobalExceptionHandler;
import com.legalassist.service.DocumentService;
import com.legalassist.service.rag.LegalQaService;
import com.legalassist.service.search.SemanticSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentController.class)
@Import(GlobalExceptionHandler.class)
class DocumentControllerAskTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private SemanticSearchService semanticSearchService;

    @MockitoBean
    private LegalQaService legalQaService;

    @Test
    @DisplayName("POST /api/documents/{id}/ask should return 200 OK and LegalAnswerResponse when grounded answer exists")
    void askQuestionShouldReturn200AndAnswer() throws Exception {
        UUID docId = UUID.randomUUID();
        CitationResponse citation = new CitationResponse(7, 12, "Either party may terminate on 30 days notice.");
        LegalAnswerResponse response = new LegalAnswerResponse(
                "Either party may terminate the agreement with 30 days notice.",
                true,
                List.of(citation)
        );

        when(legalQaService.askQuestion(eq(docId), any())).thenReturn(response);

        String jsonReq = """
                {
                    "question": "What are the termination conditions?"
                }
                """;

        mockMvc.perform(post("/api/documents/{id}/ask", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReq)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Either party may terminate the agreement with 30 days notice."))
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.citations.length()").value(1))
                .andExpect(jsonPath("$.citations[0].pageNumber").value(7))
                .andExpect(jsonPath("$.citations[0].chunkIndex").value(12))
                .andExpect(jsonPath("$.citations[0].excerpt").value("Either party may terminate on 30 days notice."));

        verify(legalQaService).askQuestion(eq(docId), any());
    }

    @Test
    @DisplayName("POST /api/documents/{id}/ask should return 400 Bad Request when question is blank")
    void askQuestionShouldReturn400WhenQuestionIsBlank() throws Exception {
        UUID docId = UUID.randomUUID();
        String jsonReq = """
                {
                    "question": "   "
                }
                """;

        mockMvc.perform(post("/api/documents/{id}/ask", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReq)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("question: Question must not be blank"));
    }

    @Test
    @DisplayName("POST /api/documents/{id}/ask should return 404 Not Found when document does not exist")
    void askQuestionShouldReturn404WhenNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(legalQaService.askQuestion(eq(missingId), any()))
                .thenThrow(new DocumentNotFoundException(missingId));

        String jsonReq = """
                {
                    "question": "What is the agreement term?"
                }
                """;

        mockMvc.perform(post("/api/documents/{id}/ask", missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReq)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Document not found with id: " + missingId));
    }

    @Test
    @DisplayName("POST /api/documents/ask should return 200 OK for multi-document ask request")
    void askMultiDocumentQuestionShouldReturn200() throws Exception {
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();
        CitationResponse citationA = new CitationResponse(docA, 1, 2, "Doc A excerpt");
        CitationResponse citationB = new CitationResponse(docB, 3, 4, "Doc B excerpt");

        LegalAnswerResponse response = new LegalAnswerResponse(
                "Combined answer for Doc A and Doc B",
                true,
                List.of(citationA, citationB)
        );

        when(legalQaService.askMultiDocumentQuestion(eq(List.of(docA, docB)), any())).thenReturn(response);

        String jsonReq = String.format("""
                {
                    "question": "What are the termination terms?",
                    "documentIds": ["%s", "%s"]
                }
                """, docA, docB);

        mockMvc.perform(post("/api/documents/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReq)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Combined answer for Doc A and Doc B"))
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.citations.length()").value(2))
                .andExpect(jsonPath("$.citations[0].documentId").value(docA.toString()))
                .andExpect(jsonPath("$.citations[1].documentId").value(docB.toString()));

        verify(legalQaService).askMultiDocumentQuestion(eq(List.of(docA, docB)), any());
    }

    @Test
    @DisplayName("POST /api/documents/ask should return 400 Bad Request when documentIds is empty")
    void askMultiDocumentQuestionShouldReturn400WhenEmptyDocIds() throws Exception {
        String jsonReq = """
                {
                    "question": "What are the termination terms?",
                    "documentIds": []
                }
                """;

        mockMvc.perform(post("/api/documents/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReq)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Please select at least one document before asking a question."));
    }
}
