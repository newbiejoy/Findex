package com.findex.service;

import com.findex.core.model.BenchmarkResult;
import com.findex.core.model.IndexedDocument;
import com.findex.core.model.SearchResponse;
import com.findex.core.retrieval.LinearScanEngine;
import com.findex.core.retrieval.SearchEngine;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class BenchmarkService {
    private final IndexingService indexingService;
    private final SearchEngine searchEngine;
    private final LinearScanEngine linearEngine;

    public BenchmarkService(IndexingService indexingService) {
        this.indexingService = indexingService;
        this.searchEngine = new SearchEngine(indexingService.getIndex());
        this.linearEngine = new LinearScanEngine();
    }

    public BenchmarkResult runBenchmark(String query) {
        Collection<IndexedDocument> allDocs = indexingService.getIndex().getAllDocuments();
        if (query == null || query.isBlank()) {
            query = "java";
        }

        searchEngine.search(query, "bm25", null, null, 10);
        linearEngine.search(allDocs, query, 10);

        int iterations = 10;
        long startInverted = System.nanoTime();
        SearchResponse invertedResponse = null;
        for (int i = 0; i < iterations; i++) {
            invertedResponse = searchEngine.search(query, "bm25", null, null, 10);
        }
        double invertedTimeMs = (System.nanoTime() - startInverted) / (iterations * 1_000_000.0);

        long startLinear = System.nanoTime();
        SearchResponse linearResponse = null;
        for (int i = 0; i < iterations; i++) {
            linearResponse = linearEngine.search(allDocs, query, 10);
        }
        double linearTimeMs = (System.nanoTime() - startLinear) / (iterations * 1_000_000.0);

        double effectiveInverted = Math.max(0.001, invertedTimeMs);
        double speedup = linearTimeMs / effectiveInverted;

        return new BenchmarkResult(
            query,
            Math.round(linearTimeMs * 1000.0) / 1000.0,
            Math.round(invertedTimeMs * 1000.0) / 1000.0,
            Math.round(speedup * 10.0) / 10.0,
            invertedResponse != null ? invertedResponse.getTotalResults() : 0,
            allDocs.size(),
            invertedResponse != null ? invertedResponse.getResults() : java.util.Collections.emptyList()
        );
    }
}
