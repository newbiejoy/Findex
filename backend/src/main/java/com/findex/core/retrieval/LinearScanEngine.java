package com.findex.core.retrieval;

import com.findex.core.model.IndexedDocument;
import com.findex.core.model.SearchResponse;
import com.findex.core.model.SearchResult;
import com.findex.core.snippet.SnippetGenerator;

import java.util.*;

public class LinearScanEngine {
    private final SnippetGenerator snippetGenerator;

    public LinearScanEngine() {
        this(new SnippetGenerator());
    }

    public LinearScanEngine(SnippetGenerator snippetGenerator) {
        this.snippetGenerator = snippetGenerator;
    }

    public SearchResponse search(Collection<IndexedDocument> documents, String rawQuery, int limit) {
        long startTime = System.nanoTime();

        if (rawQuery == null || rawQuery.isBlank()) {
            return new SearchResponse("", "LinearScan", 0, 0.0, Collections.emptyList());
        }

        String lowerQuery = rawQuery.trim().toLowerCase(Locale.ROOT);
        String[] terms = lowerQuery.split("\\s+");
        Set<String> termSet = new HashSet<>(Arrays.asList(terms));

        List<SearchResult> matches = new ArrayList<>();

        for (IndexedDocument doc : documents) {
            String text = doc.getPreviewText();
            if (text == null) continue;
            String lowerText = text.toLowerCase(Locale.ROOT);

            int matchCount = 0;
            List<String> matchedTerms = new ArrayList<>();

            for (String term : terms) {
                int count = 0;
                int idx = lowerText.indexOf(term);
                while (idx >= 0) {
                    count++;
                    idx = lowerText.indexOf(term, idx + term.length());
                }
                if (count > 0) {
                    matchCount += count;
                    matchedTerms.add(term);
                }
            }

            if (matchCount > 0) {
                double score = (double) matchCount / Math.max(1, doc.getTermCount());
                String snippet = snippetGenerator.generateSnippet(text, termSet);
                matches.add(new SearchResult(
                    doc,
                    Math.round(score * 1000.0) / 1000.0,
                    matchedTerms,
                    snippet,
                    matchCount,
                    "Linear frequency count: " + matchCount
                ));
            }
        }

        Collections.sort(matches);

        int totalFound = matches.size();
        if (limit > 0 && matches.size() > limit) {
            matches = matches.subList(0, limit);
        }

        double timeTakenMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return new SearchResponse(rawQuery, "LinearScan", totalFound, Math.round(timeTakenMs * 100.0) / 100.0, matches);
    }
}
