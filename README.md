# Findex — Local-First Document Search Engine

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

A high-performance, privacy-respecting, local-first document information retrieval (IR) system designed for developer notes, source code, text files, Markdown documents, and PDFs.

---

## Key Features

- **AST-Based Boolean Query Parser**: Supports recursive Boolean expressions:
  - `AND` queries (e.g. `java AND classes`)
  - `OR` queries (e.g. `algorithms OR inheritance`)
  - `NOT` / prefix `-` exclusions (e.g. `programming -python`)
  - Exact positional phrases with quotes (e.g. `"classes and inheritance"`)
- **Multiple Ranking Models**:
  - **Okapi BM25** ($k_1=1.2, b=0.75$) with document length normalization and inverse document frequency (IDF).
  - **TF-IDF** scoring with sub-linear log-frequency weighting.
  - Transparent score explanations showing IDF, normalized TF, and breakdown per query term.
- **Inverted Index Engine**: Thread-safe in-memory postings list with term positions, document frequencies, and document lengths, backed by SQLite persistence.
- **Empirical Benchmarking**: Side-by-side performance comparison against a sequential Linear Scan baseline engine, measuring exact query execution latency and speedup factor.
- **Modern Minimalist UI**: Clean, developer-focused interface with keyboard shortcut navigation (`/` to focus, `Esc` to clear), highlighted keyword snippets (`<mark>`), directory management, and live indexing indicators.

---

## Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3.5 LTS, SQLite JDBC, Apache PDFBox, Maven |
| **Frontend** | React 18, TypeScript, Vite, Vanilla CSS design system, Lucide Icons |
| **Database** | SQLite (local file: `findex.db`) |

---

## Architecture Overview

```
findex/
├── backend/                             # Java 21 Spring Boot Backend
│   ├── src/main/java/com/findex/
│   │   ├── core/                        # Information Retrieval Core
│   │   │   ├── index/                   # InvertedIndex (postings, locks, frequencies)
│   │   │   ├── ranking/                 # BM25 & TF-IDF scorers
│   │   │   ├── retrieval/               # AST QueryParser & SearchEngine
│   │   │   ├── snippet/                 # SnippetGenerator with HTML highlights
│   │   │   └── tokenizer/               # StandardTokenizer with stopword support
│   │   ├── service/                     # IndexingService, BenchmarkService, SearchService
│   │   ├── controller/                  # REST Controllers (/api/search, /api/folders, etc.)
│   │   └── storage/                     # SQLite DatabaseManager
│   └── src/test/java/com/findex/        # Automated JUnit 5 test suite
├── frontend/                            # React 18 + TypeScript + Vite UI
│   ├── src/
│   │   ├── App.tsx                      # Main application with Search, Folders, Benchmark
│   │   ├── api.ts                       # Typed API client
│   │   └── index.css                    # Modern developer-tool CSS design system
└── documents/                           # Sample document collection for indexing
```

---

## Getting Started

### Prerequisites

- **Java Development Kit (JDK) 21** or later
- **Node.js 18+** & `npm`

### 1. Run the Backend

```bash
cd backend
./mvnw clean spring-boot:run
```

The Spring Boot REST API will start on `http://localhost:8080`.

### 2. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

Open your browser at `http://localhost:5173`.

---

## Automated Tests

To run the automated JUnit 5 test suite covering tokenization, indexing, ranking, Boolean AST queries, and snippet generation:

```bash
cd backend
./mvnw test
```

---

## REST API Reference

| Endpoint | Method | Description |
|---|---|---|
| `/api/health` | `GET` | Service health status and version info |
| `/api/search?q={query}&mode={bm25\|tfidf}&type={ext}` | `GET` | Search documents with ranking and snippets |
| `/api/suggestions?q={prefix}` | `GET` | Keyword auto-complete suggestions |
| `/api/folders` | `GET` | List all registered folders |
| `/api/folders` | `POST` | Register a new local directory for indexing |
| `/api/folders/{id}` | `DELETE` | Remove a registered directory |
| `/api/folders/{id}/reindex` | `POST` | Trigger asynchronous reindexing of a folder |
| `/api/index/status` | `GET` | Real-time indexing progress and state |
| `/api/stats` | `GET` | Total documents, unique terms, postings, and JVM memory |
| `/api/benchmark?q={query}` | `POST` | Run empirical benchmark against Linear Scan |
| `/api/history` | `GET` | Query execution history log |
