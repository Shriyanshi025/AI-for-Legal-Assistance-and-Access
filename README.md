# AI for Legal Assistance & Access

## Overview

**AI for Legal Assistance & Access** is an end-to-end, privacy-focused legal document analysis and question-answering platform. It allows users to upload legal PDF documents, process and index their contents through a multi-stage pipeline, and ask legal questions that yield grounded answers backed by page- and chunk-level citations.

---

## Problem

Legal contracts, agreements, and regulatory documents are often complex, lengthy, and difficult to navigate. Non-experts and legal professionals alike spend substantial time searching for specific clauses, notice periods, termination conditions, or compliance mandates. Generic AI language models frequently hallucinate or generate plausible-sounding answers without providing verifiable source evidence, introducing risk in legal contexts.

---

## Solution

This platform addresses legal document complexity through a **Grounded Retrieval-Augmented Generation (RAG)** pipeline. By combining deterministic text chunking, 768-dimensional vector embeddings, PostgreSQL `pgvector` similarity search, and a strict citation validation engine, the system ensures that generated legal answers are directly derived from and traceable back to exact pages and chunks of the user's uploaded document.

---

## Key Capabilities

* **Legal Document Upload**: Secure PDF document upload and metadata tracking.
* **PDF Text Extraction**: Machine-readable text extraction with page boundary preservation (Apache PDFBox).
* **Deterministic Text Chunking**: Page-aware legal clause and section chunking with configurable overlap.
* **Semantic Embedding Generation**: Google Gemini 768-dimensional L2-normalized vector embeddings (`gemini-embedding-001`).
* **Vector Similarity Search**: High-performance PostgreSQL + `pgvector` cosine similarity search.
* **Relevance Gating**: Configurable minimum similarity threshold (0.35) preventing weak or unrelated context from invoking LLM generation.
* **Grounded Legal Answer Generation**: Fact-based legal answer synthesis using Google Gemini (`gemini-3.5-flash`).
* **Source Provenance & Citation Validation**: Strict verification mapping output citations to backend database records (`pageNumber`, `chunkIndex`, `excerpt`).
* **Insufficient Context Detection**: Deterministic fallback messaging when a document does not contain relevant information to answer a query.
* **Interactive Web Interface**: Modern, responsive React + TypeScript frontend with drag-and-drop upload, pipeline status tracking, and citation cards.

---

## System Architecture

```text
Legal PDF Document
        ↓
POST /api/documents (Upload & Supabase Storage)
        ↓
POST /api/documents/{id}/extract (Apache PDFBox Page Text Extraction)
        ↓
POST /api/documents/{id}/chunks (Legal Text Chunking)
        ↓
POST /api/documents/{id}/embeddings (Gemini 768-d Vector Embeddings)
        ↓
PostgreSQL + pgvector (Semantic Storage & Indexing)
        ↓
POST /api/documents/{id}/ask (User Question)
        ↓
Cosine Similarity Retrieval (Document-Scoped Search)
        ↓
Relevance Gate (minSimilarity >= 0.35)
        ↓
Gemini Grounded Answer Generation (gemini-3.5-flash)
        ↓
Backend Citation Validation (Source Provenance Mapping)
        ↓
Legal Answer + Page & Chunk Citations
```

---

## Technology Stack

### Backend
* **Language & Runtime**: Java 21
* **Framework**: Spring Boot 3.4.1
* **Build Tool**: Apache Maven
* **Database**: PostgreSQL with `pgvector` extension
* **Storage**: Supabase Storage Integration
* **PDF Processing**: Apache PDFBox 3.0.3
* **AI Provider**: Google Gemini REST API (`gemini-embedding-001`, `gemini-3.5-flash`)
* **API Style**: RESTful JSON API

### Frontend
* **Framework**: React 18
* **Language**: TypeScript
* **Build Tool**: Vite 8
* **HTTP Client**: Native `fetch` API
* **Styling**: Vanilla CSS (Custom Properties Design System, Responsive Grid, Glassmorphism UI)

---

## Legal-Specific Design

1. **Assistance over Replacement**: The system is explicitly engineered to **assist** users in analyzing their own uploaded documents. It does not provide automated legal representation or ungrounded legal opinions.
2. **Strict Grounding Contract**: Prompt instructions constrain the AI to answer strictly based on the provided document context.
3. **Verifiable Provenance**: Every answer includes detailed citation tags showing:
   * Exact Page Number (`pageNumber`)
   * Chunk Index (`chunkIndex`)
   * Source Excerpt (`excerpt`)
4. **Insufficient Context Protection**: If top-K vector search results fall below the relevance threshold (0.35), the system declines to invoke the LLM and returns: *"The provided document does not contain enough information to answer this question."*

---

## Backend API

| Endpoint | Method | Request Body / Query | Response DTO | Description |
| :--- | :--- | :--- | :--- | :--- |
| `/api/documents` | `POST` | `multipart/form-data`: `file`, `userId` | `DocumentResponse` | Upload PDF document |
| `/api/documents` | `GET` | `userId` (query param) | `List<DocumentSummaryResponse>` | List user's documents |
| `/api/documents/{id}` | `GET` | None | `DocumentResponse` | Fetch document metadata |
| `/api/documents/{id}/extract` | `POST` | None | `DocumentResponse` | Extract PDF text & pages |
| `/api/documents/{id}/pages` | `GET` | None | `List<DocumentPageResponse>` | Get extracted pages |
| `/api/documents/{id}/chunks` | `POST` | None | `List<DocumentChunkResponse>` | Create text chunks |
| `/api/documents/{id}/chunks` | `GET` | None | `List<DocumentChunkResponse>` | Get text chunks |
| `/api/documents/{id}/embeddings` | `POST` | None | `List<DocumentChunkResponse>` | Generate 768-d embeddings |
| `/api/documents/{id}/search` | `POST` | `{"query": "...", "topK": 5}` | `List<SimilaritySearchResultResponse>` | Vector similarity search |
| `/api/documents/{id}/ask` | `POST` | `{"question": "..."}` | `LegalAnswerResponse` | Ask grounded legal Q&A |

---

## Frontend

The frontend is a single-page web application (`legal-assistance-frontend`) built with React and TypeScript. Key features include:

* **Document Management Sidebar**: Drag-and-drop uploader, real-time library listing, and status indicators (`UPLOADED`, `PROCESSING`, `READY`, `FAILED`).
* **One-Click Document Preparation**: Single-click pipeline button executing text extraction, chunking, and embedding generation sequentially.
* **Legal Q&A Workspace**: Question entry panel with character validation (1000-character limit).
* **Grounded Answer & Citation Presentation**: Answer container displaying a Grounded Badge (emerald green) or Insufficient Context Alert (amber/orange), accompanied by source citation cards.

---

## AI / RAG Pipeline

1. **Embedding Generation**: Uses `gemini-embedding-001` configured with `output_dimensionality = 768` and task type `RETRIEVAL_DOCUMENT` for chunk indexing, and `RETRIEVAL_QUERY` for search queries. Vectors are L2-normalized once inside `GeminiEmbeddingServiceImpl`.
2. **Relevance Gating**: Retrieves top-K chunks via PostgreSQL `pgvector` cosine similarity (`<=>` operator) and filters out candidates below `app.rag.min-similarity` (default 0.35).
3. **LLM Generation**: Constructs a bounded system prompt instructing `gemini-3.5-flash` to cite source items using exact `[SRC-N]` tags.
4. **Citation Validation**: `CitationValidator` cross-references returned citation IDs with internal `RagContext` records. If no valid citations match, `grounded` is set to `false`.

---

## Security & Privacy

* **Server-Side Secret Isolation**: `GEMINI_API_KEY` is loaded exclusively on the backend via environment variables or an untracked `.env` file (`spring.config.import=optional:file:.env[.properties]`).
* **Zero Secret Exposure**: The frontend contains **no** Gemini API keys, SDKs, or direct network calls to Google APIs.
* **Git Safety**: `.gitignore` rules prevent `.env`, build artifacts (`target/`, `dist/`), and `node_modules/` from being committed.
* **Prompt Injection Safeguards**: User questions are delimited from document context within prompt templates to prevent prompt injection hijacking.

---

## Local Development

### Prerequisites
* Java 21 JDK
* Node.js 18+ & npm
* PostgreSQL database with `pgvector` extension (or Supabase instance)
* Google Gemini API Key

### 1. Backend Setup

```powershell
cd legal-assistance-backend

# Create untracked .env file with your secrets
# (See .env.example for required keys)
echo "GEMINI_API_KEY=your_gemini_api_key_here" > .env

# Run backend
.\mvnw.cmd spring-boot:run
```
The backend starts at `http://localhost:8080`.

### 2. Frontend Setup

```powershell
cd legal-assistance-frontend

# Install dependencies
npm install

# Start development server
npm run dev
```
The frontend starts at `http://localhost:5173`.

---

## Project Structure

```text
AI-for-Legal-Assistance-and-Access/
├── .gitignore                      # Root Git ignore rules
├── README.md                       # Main project documentation
├── legal-assistance-backend/       # Spring Boot Java 21 backend application
│   ├── pom.xml                     # Maven dependencies
│   ├── .env.example                # Environment variables template
│   └── src/                        # Spring Boot controllers, services, entities, DTOs
└── legal-assistance-frontend/      # React 18 + Vite + TypeScript frontend application
    ├── package.json                # npm dependencies & scripts
    ├── vite.config.ts              # Vite bundler configuration
    └── src/                        # React components, views, API client, & CSS styles
```

---

## Current Limitations

* **Pre-Authentication Contract**: `userId` parameter is currently passed as a temporary pre-authentication query parameter prior to full Spring Security implementation.
* **Scanned PDF Support**: Text extraction currently relies on PDFBox text streams. Scanned or image-only PDFs without OCR streams will set status to `FAILED`.
* **Single-Document Scoped Search**: Q&A operates on a per-document basis (`POST /api/documents/{id}/ask`). Multi-document corpus search is planned for future releases.

---

## Future Enhancements

* Integrated OCR pipeline (Tesseract / Cloud Vision) for scanned PDF support.
* Spring Security JWT authentication context replacing temporary `userId` parameter.
* Multi-document comparative legal analysis and corpus-wide retrieval.

---

## Disclaimer

*This application is an AI-powered informational tool designed to assist users in navigating and retrieving information from their own legal documents. It does not provide formal legal advice, legal representation, or binding legal interpretation. Users should consult a qualified legal professional for official advice regarding legal contracts and agreements.*
