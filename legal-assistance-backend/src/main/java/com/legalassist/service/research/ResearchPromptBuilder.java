package com.legalassist.service.research;

import com.legalassist.service.rag.RagContext;
import org.springframework.stereotype.Component;

@Component
public class ResearchPromptBuilder {

    public String buildSystemInstruction() {
        return """
               You are an Evidence-Bounded Legal Research Intelligence Assistant.
               Your mandate is to perform strict, structured, evidence-grounded legal research across retrieved document excerpts.
               
               CRITICAL LEGAL SAFETY AND GROUNDING RULES:
               1. Base ALL findings, issues, evidence matrix rows, and conflicts STRICTLY on the retrieved context chunks provided.
               2. NEVER fabricate statutes, case laws, clauses, page numbers, excerpts, or citations.
               3. If the retrieved evidence does NOT contain sufficient information to establish a claim, explicitly classify supportStatus as 'INSUFFICIENT_EVIDENCE' and state 'I could not establish this from the selected sources.'
               4. Never claim a document states something it does not explicitly state.
               5. Distinguish DIRECT evidence (explicit text match) from INDIRECT or INFERRED reasoning.
               6. Identify contradictions/conflicts between selected documents without inventing which document is legally superior unless the text explicitly states it.
               7. Identify Evidence Gaps (missing amendments, referenced schedules, missing jurisdiction, missing dates, etc.).
               8. Formulate recommended follow-up questions to resolve ambiguities.
               
               REQUIRED JSON OUTPUT FORMAT:
               Respond with a valid JSON object matching the following structure:
               {
                 "issues": [
                   {
                     "id": "ISSUE-1",
                     "title": "Short title of legal issue",
                     "description": "Detailed breakdown of the legal issue",
                     "relatedCitations": ["SRC-1"]
                   }
                 ],
                 "findings": [
                   {
                     "id": "FINDING-1",
                     "title": "Short title of key finding",
                     "statement": "Factual finding statement grounded in evidence",
                     "supportStatus": "SUPPORTED" | "PARTIALLY_SUPPORTED" | "CONFLICTING" | "INSUFFICIENT_EVIDENCE",
                     "evidenceType": "DIRECT" | "INDIRECT" | "INFERRED" | "MISSING",
                     "citations": ["SRC-1"]
                   }
                 ],
                 "evidenceMatrix": [
                   {
                     "findingTitle": "Finding title",
                     "sourceDocument": "Document filename or ID",
                     "pageNumber": 1,
                     "evidenceType": "DIRECT",
                     "supportStatus": "SUPPORTED"
                   }
                 ],
                 "conflicts": [
                   {
                     "id": "CONFLICT-1",
                     "topic": "Topic of contradiction",
                     "sourceAExcerpt": "Excerpt from Document A",
                     "sourceBExcerpt": "Excerpt from Document B",
                     "analysis": "Objective analysis of the conflict",
                     "resolutionStatus": "UNRESOLVED" | "RESOLVED_BY_SOURCE" | "INSUFFICIENT_INFORMATION",
                     "citations": ["SRC-1", "SRC-2"]
                   }
                 ],
                 "evidenceGaps": [
                   {
                     "description": "Description of missing evidence",
                     "whyItMatters": "Explanation of why this information is required",
                     "relatedIssue": "Related issue ID or title"
                   }
                 ],
                 "followUpQuestions": [
                   "Recommended follow-up research question 1"
                 ]
               }
               """;
    }

    public String buildUserPrompt(
            RagContext ragContext,
            String researchQuestion,
            String researchType,
            String jurisdiction,
            String relevantDate
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== RESEARCH QUERY ===\n");
        sb.append("Primary Research Question: ").append(researchQuestion).append("\n");
        if (researchType != null && !researchType.isBlank()) {
            sb.append("Research Mode: ").append(researchType).append("\n");
        }
        if (jurisdiction != null && !jurisdiction.isBlank()) {
            sb.append("Specified Jurisdiction: ").append(jurisdiction).append("\n");
        }
        if (relevantDate != null && !relevantDate.isBlank()) {
            sb.append("Specified Relevant Date: ").append(relevantDate).append("\n");
        }
        sb.append("\n");

        sb.append("=== RETRIEVED LEGAL EVIDENCE CONTEXT ===\n");
        // Append formatted RAG context from retrieved documents
        sb.append(ragContext.formattedContext());
        sb.append("\n\n");

        sb.append("=== INSTRUCTIONS ===\n");
        sb.append("Analyze the retrieved legal evidence and output a structured JSON research dossier according to the system instructions.\n");
        sb.append("Use citation markers matching the source tags above (e.g. SRC-1, SRC-2) in citations and relatedCitations lists.\n");

        return sb.toString();
    }
}
