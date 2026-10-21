PROJECT PROPOSAL

FINDEX

A Local-First Document Search Engine

A Java-powered information retrieval system with a React interface

Project category

Full-stack software / Information Retrieval

Primary language

Java 21

Frontend

React + TypeScript

Planned duration

5 weeks for the first complete version

Prepared as a project planning document

1. Executive Summary

Findex is a local-first document search application designed to help users find relevant information across personal programming notes, text files, Markdown documents, source code, and selected document formats. Unlike a basic file-name search tool, Findex will analyze document content, build an inverted index, retrieve candidate documents efficiently, and rank results by relevance.

The project combines a search engine implemented primarily in Java with a React-based user interface. The first version will prioritize a transparent, testable information-retrieval engine. Spring Boot will expose the engine through a local REST API, while SQLite will persist document metadata and application settings. Advanced features will be added only after the core search pipeline is correct and measurable.

2. Problem Statement

Personal collections of notes, documentation, code snippets, and PDFs can become difficult to search as they grow. Browsing folders or searching filenames does not reliably locate information buried inside documents. A useful solution must index content, retrieve matching documents efficiently, rank results meaningfully, and provide enough context for users to identify the right source.

3. Proposed Solution

Findex will allow users to register approved local folders, index supported files, submit search queries, inspect ranked results, and open the original documents. It will also provide an index-management view and basic performance statistics. The system will run locally by default and will not require a cloud account.

4. Objectives

Implement a working inverted index using Java collections and clearly defined document identifiers.

Build a text-processing pipeline for tokenization, normalization, and term statistics.

Implement multi-term retrieval and relevance ranking, beginning with TF-IDF.

Provide a React interface for search, result inspection, index management, and statistics.

Expose search and indexing operations through a Spring Boot REST API.

Persist document metadata and settings locally, then add index persistence after the core engine is validated.

Create automated tests and reproducible benchmarks to evaluate correctness and performance.

Produce technical documentation describing architecture, algorithms, limitations, and future work.

5. Scope and Feature Set

5.1 Minimum Viable Product (MVP)

Register a user-approved local folder for indexing.

Read .txt, .md, and .java files.

Extract text, normalize tokens, and build an inverted index.

Search for one or more terms and return matching documents.

Rank results using TF-IDF or a clearly documented equivalent scoring model.

Display result title, path, snippet, matched terms, and relevance score.

Show indexing status, indexed document count, and basic query latency.

Handle empty queries, unreadable files, duplicate indexing, and unsupported extensions.

Provide automated unit tests for tokenization, indexing, retrieval, and ranking.

5.2 Planned Enhancements

Phrase search and Boolean operators such as AND, OR, and NOT.

BM25 ranking and comparison with the initial TF-IDF implementation.

File-type, path, and modification-date filters.

Incremental re-indexing and file-change detection.

PDF and DOCX text extraction using Apache Tika.

Persistent index restoration after application restart.

Index explorer showing terms, document frequency, and postings.

Performance dashboard and search-quality evaluation.

Fuzzy search, query suggestions, and advanced source-code search.

5.3 Out of Scope for the First Release

Internet-scale web crawling or a replacement for commercial search engines.

Cloud synchronization, user accounts, and multi-user collaboration.

AI chatbot or embedding-based semantic search.

OCR for scanned documents.

Distributed indexing and microservice architecture.

Unrestricted filesystem access or remote exposure of the local API.

6. Technology Stack

Area

Technology

Purpose

Core engine

Java 21

Document processing, inverted index, retrieval, scoring, and concurrency extensions.

Backend API

Spring Boot + Maven

REST endpoints, validation, error handling, configuration, and service coordination.

Frontend

React + TypeScript + Vite

Search interface, result views, filters, index management, and analytics.

UI and client state

Tailwind CSS; React Router; TanStack Query; optional Zustand

Styling, navigation, API state, and small shared UI state.

Local persistence

SQLite + JDBC

Document metadata, registered folders, settings, and search history.

Document extraction

Java NIO initially; Apache Tika later

Read supported files and extract text from additional document formats.

Testing

JUnit 5; Mockito; Vitest; React Testing Library

Unit, service, and frontend behavior tests.

Version control

Git + GitHub

Version history, issue tracking, and portfolio presentation.

Implementation principle: the initial search engine will be custom-built for learning purposes. Apache Lucene may be evaluated later as a comparison baseline, but it will not replace the core inverted-index implementation in the first release.

7. High-Level Architecture

FRONTEND — React + TypeScriptSearch page • results • filters • index explorer • statistics

↓ REST/JSON requests to local Spring Boot API ↓

BACKEND — Spring BootControllers • validation • services • error handling

↓ calls the engine through Java interfaces ↓

SEARCH ENGINE + LOCAL STORAGETokenizer • inverted index • query processing • ranking • file extraction • SQLite

The search engine should remain independent of Spring Boot wherever practical, allowing it to be tested directly from Java tests without starting a web server. The application will initially run on the user's own computer.

8. Core Search Algorithm

8.1 Indexing pipeline

Discover files only within user-approved folders and validate file types and sizes.

Extract text and normalize it consistently.

Tokenize text into terms; optionally remove stop words only after evaluating the effect on search quality.

Assign stable document identifiers and record document metadata.

Build an inverted index mapping each term to the documents and term statistics in which it appears.

Store enough metadata to update or remove a document from the index when its source changes.

8.2 Query pipeline

Validate and normalize the query using the same tokenization rules as indexing.

Retrieve candidate documents from the relevant postings lists.

Calculate a relevance score using TF-IDF for the initial implementation.

Sort or select the highest-scoring results and generate snippets from matching text.

Return results with timing information and optional ranking explanations.

After the basic pipeline is correct, BM25 can be implemented and evaluated against TF-IDF. Ranking quality will be measured on a small, manually labelled set of queries and relevant documents rather than judged only by visual output.

9. API Plan

Method

Endpoint

Purpose

GET

/api/health

Check local service availability.

POST

/api/indexes

Register a permitted local folder.

POST

/api/indexes/{id}/rebuild

Request a full re-index.

GET

/api/indexes/{id}/status

Read indexing progress and errors.

GET

/api/search?q=...

Search indexed documents.

GET

/api/documents/{id}

Read document metadata.

GET

/api/stats

Read index and query statistics.

GET

/api/history

Read recent search history.

The endpoint list is a proposed interface; names and response schemas may be refined during implementation. Long-running indexing should expose status rather than blocking a request until every file has been processed.

10. Development Schedule (5 Weeks)

Week 1 — Java engine foundation

Read text files, tokenize content, assign document IDs, build the inverted index, and implement basic single-term and multi-term retrieval. Deliverable: a working console search engine.

Week 2 — Ranking and tests

Implement TF-IDF, result ordering, snippets, edge-case handling, unit tests, and baseline timing measurements. Deliverable: tested ranked search.

Week 3 — Spring Boot and React

Expose core operations through REST endpoints and build the search UI, result cards, loading/error states, and index status. Deliverable: end-to-end search through the browser.

Week 4 — Persistence and documents

Add SQLite metadata/settings, restore index state where feasible, add PDF/DOCX extraction with Apache Tika, and handle re-indexing. Deliverable: a useful local tool.

Week 5 — Quality and presentation

Add selected advanced search features, run benchmarks, fix edge cases, document architecture and limitations, and prepare screenshots/demo. Deliverable: a portfolio-ready release candidate.

11. Testing and Evaluation

Unit tests: tokenizer normalization, postings-list construction, query matching, score calculation, and ranking order.

Integration tests: indexing files and retrieving results through the API.

Failure tests: unreadable files, unsupported types, empty queries, deleted files, malformed requests, and interrupted indexing.

Search-quality evaluation: maintain a small set of queries with expected relevant documents; calculate Precision@K or nDCG@K where practical.

Performance evaluation: measure index-build time, query latency, memory use, and index size on a documented corpus.

Comparison baseline: compare the custom index with a simple linear-scan implementation; compare with Lucene only as an optional later experiment.

12. Security, Privacy, and Reliability

Run the service on localhost by default and do not expose it to the public internet in the initial version.

Require explicit folder registration before indexing or opening files.

Normalize and validate filesystem paths; prevent path traversal and reject access outside approved roots, including through symbolic links.

Set reasonable file-size and processing limits; report extraction failures without crashing the indexing job.

Do not upload document contents to third-party services. Document any future feature that changes this local-first privacy model.

Use parameterized SQL queries and validate API inputs.

13. Risks and Mitigation

Risk

Mitigation

Scope growth

Freeze the MVP; add advanced features only after the core engine and tests are complete.

Poor ranking quality

Create a small labelled query set and compare scoring methods.

Slow or memory-heavy indexing

Measure performance, process incrementally, and introduce concurrency only after correctness.

Document format complexity

Start with plain text, Markdown, and Java; add PDF/DOCX extraction through a library.

Index persistence complexity

Persist metadata first, then implement and test index restoration as a separate milestone.

Filesystem security

Limit roots, validate resolved paths, and keep the local API inaccessible from other machines by default.

14. Expected Deliverables

Working Java search engine with a custom inverted index and relevance-ranking implementation.

React web interface connected to a local Spring Boot REST API.

Local document metadata/settings storage and supported-file extraction.

Automated tests and reproducible benchmark instructions.

Architecture diagram, API documentation, setup instructions, and known limitations.

Git repository with meaningful commits and a README containing screenshots and a demo walkthrough.

15. Success Criteria

A user can register an approved folder and index supported files without manual data entry.

A query returns the expected matching documents and ranks them using the documented scoring algorithm.

Search results include usable source paths and contextual snippets.

The application handles common file and query errors without terminating unexpectedly.

Metadata and configured folders survive an application restart; index restoration is implemented or explicitly documented as incomplete.

Tests and benchmark instructions can be followed by another developer using the README.

16. Future Enhancements

BM25 ranking, fuzzy matching, query suggestions, and more expressive query syntax.

Incremental background indexing using Java executors and filesystem change notifications.

Source-code-aware indexing for class names, methods, and identifiers.

Optional desktop packaging using a suitable wrapper after the web-based local version is stable.

An experimental comparison against Apache Lucene.

Semantic retrieval as an optional, separately evaluated extension rather than a dependency of the core engine.

17. Conclusion

Findex is designed as a practical local utility and a learning-focused software engineering project. Its main contribution is a transparent Java information-retrieval engine, supported by a usable React interface and a small local backend. By prioritizing indexing, retrieval, ranking, testing, and measurable performance before advanced features, the project can remain achievable while demonstrating meaningful Java, data-structure, algorithm, API, and frontend skills.

Suggested Repository Name

findex