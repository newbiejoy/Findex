package com.findex.service;

import com.findex.core.model.SearchResponse;
import com.findex.core.model.SearchResult;
import com.findex.core.retrieval.SearchEngine;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EvaluationService {
    private final IndexingService indexingService;
    private final SearchEngine searchEngine;

    public EvaluationService(IndexingService indexingService) {
        this.indexingService = indexingService;
        this.searchEngine = new SearchEngine(indexingService.getIndex());
    }

    public static class TestCase {
        private final String query;
        private final List<String> expectedKeywordsInPath;

        public TestCase(String query, List<String> expectedKeywordsInPath) {
            this.query = query;
            this.expectedKeywordsInPath = expectedKeywordsInPath;
        }

        public String getQuery() { return query; }
        public List<String> getExpectedKeywordsInPath() { return expectedKeywordsInPath; }
    }

    public Map<String, Object> evaluate(String mode, int k) {
        List<TestCase> benchmarkSuite = Arrays.asList(
            new TestCase("java", Arrays.asList("java", "Main")),
            new TestCase("data structures algorithms", Arrays.asList("dsa")),
            new TestCase("programming", Arrays.asList("java", "dsa")),
            new TestCase("class inheritance", Arrays.asList("java"))
        );

        double totalPrecisionAtK = 0.0;
        double totalReciprocalRank = 0.0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (TestCase tc : benchmarkSuite) {
            SearchResponse res = searchEngine.search(tc.getQuery(), mode, null, null, k);
            int relevantFound = 0;
            int firstRelevantRank = -1;

            for (int i = 0; i < res.getResults().size(); i++) {
                SearchResult sr = res.getResults().get(i);
                String fileName = sr.getDocument().getFileName().toLowerCase(Locale.ROOT);
                boolean isRelevant = tc.getExpectedKeywordsInPath().stream().anyMatch(fileName::contains);
                if (isRelevant) {
                    relevantFound++;
                    if (firstRelevantRank == -1) {
                        firstRelevantRank = i + 1;
                    }
                }
            }

            double precision = (res.getResults().isEmpty()) ? 0.0 : ((double) relevantFound / res.getResults().size());
            double rr = (firstRelevantRank > 0) ? (1.0 / firstRelevantRank) : 0.0;

            totalPrecisionAtK += precision;
            totalReciprocalRank += rr;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("query", tc.getQuery());
            row.put("resultsRetrieved", res.getResults().size());
            row.put("relevantInTopK", relevantFound);
            row.put("precisionAtK", Math.round(precision * 100.0) / 100.0);
            row.put("firstRelevantRank", firstRelevantRank > 0 ? firstRelevantRank : "None");
            row.put("reciprocalRank", Math.round(rr * 100.0) / 100.0);
            details.add(row);
        }

        int count = Math.max(1, benchmarkSuite.size());
        double meanPrecision = totalPrecisionAtK / count;
        double mrr = totalReciprocalRank / count;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("model", mode != null ? mode.toUpperCase() : "BM25");
        summary.put("evaluatedQueries", count);
        summary.put("k", k);
        summary.put("meanPrecisionAtK", Math.round(meanPrecision * 1000.0) / 1000.0);
        summary.put("mrr", Math.round(mrr * 1000.0) / 1000.0);
        summary.put("cases", details);

        return summary;
    }
}
