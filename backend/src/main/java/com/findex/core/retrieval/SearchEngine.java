package com.findex.core.retrieval;

import com.findex.core.index.InvertedIndex;
import com.findex.core.model.*;
import com.findex.core.ranking.BM25Scorer;
import com.findex.core.ranking.Scorer;
import com.findex.core.ranking.TfIdfScorer;
import com.findex.core.snippet.SnippetGenerator;
import com.findex.core.tokenizer.StandardTokenizer;
import com.findex.core.tokenizer.Tokenizer;

import java.util.*;

public class SearchEngine {
    private final InvertedIndex index;
    private final Tokenizer tokenizer;
    private final QueryParser queryParser;
    private final SnippetGenerator snippetGenerator;
    private final Scorer tfIdfScorer;
    private final Scorer bm25Scorer;

    public SearchEngine(InvertedIndex index) {
        this(index, new StandardTokenizer(), new SnippetGenerator());
    }

    public SearchEngine(InvertedIndex index, Tokenizer tokenizer, SnippetGenerator snippetGenerator) {
        this.index = index;
        this.tokenizer = tokenizer;
        this.queryParser = new QueryParser(tokenizer);
        this.snippetGenerator = snippetGenerator;
        this.tfIdfScorer = new TfIdfScorer();
        this.bm25Scorer = new BM25Scorer();
    }

    public SearchResponse search(String rawQuery, String rankingMode, String fileTypeFilter, String folderFilter, int limit) {
        long startTime = System.nanoTime();

        if (rawQuery == null || rawQuery.isBlank()) {
            return new SearchResponse("", rankingMode != null ? rankingMode : "bm25", 0, 0.0, Collections.emptyList());
        }

        Scorer scorer = "tfidf".equalsIgnoreCase(rankingMode) ? tfIdfScorer : bm25Scorer;

        QueryParser.Node ast = queryParser.parse(rawQuery);
        Set<Integer> candidateDocIds = queryParser.evaluateCandidates(ast, index);
        Set<String> queryTerms = queryParser.extractPositiveTerms(ast);

        if (candidateDocIds.isEmpty()) {
            double timeTakenMs = (System.nanoTime() - startTime) / 1_000_000.0;
            return new SearchResponse(rawQuery, scorer.getName(), 0, Math.round(timeTakenMs * 100.0) / 100.0, Collections.emptyList());
        }

        List<IndexedDocument> filteredDocs = new ArrayList<>();
        for (int docId : candidateDocIds) {
            IndexedDocument doc = index.getDocument(docId);
            if (doc == null) continue;

            if (fileTypeFilter != null && !fileTypeFilter.isBlank()) {
                String expectedExt = fileTypeFilter.startsWith(".") ? fileTypeFilter.substring(1) : fileTypeFilter;
                if (!expectedExt.equalsIgnoreCase(doc.getExtension())) {
                    continue;
                }
            }

            if (folderFilter != null && !folderFilter.isBlank()) {
                if (!folderFilter.equals(doc.getFolderId())) {
                    continue;
                }
            }

            filteredDocs.add(doc);
        }

        int totalDocs = Math.max(index.getTotalDocuments(), 1);
        double avgDocLength = index.getAvgDocLength();
        List<SearchResult> rankedResults = new ArrayList<>();

        for (IndexedDocument doc : filteredDocs) {
            double totalScore = 0.0;
            int totalMatches = 0;
            List<String> matchedTermsList = new ArrayList<>();
            List<String> explanations = new ArrayList<>();

            for (String term : queryTerms) {
                Posting posting = index.getPosting(term, doc.getId());
                if (posting != null && posting.getTermFrequency() > 0) {
                    int docFreq = index.getDocumentFrequency(term);
                    double termScore = scorer.score(term, posting, doc.getTermCount(), totalDocs, docFreq, avgDocLength);
                    totalScore += termScore;
                    totalMatches += posting.getTermFrequency();
                    matchedTermsList.add(term);
                    explanations.add(scorer.explain(term, posting, doc.getTermCount(), totalDocs, docFreq, avgDocLength));
                }
            }

            if (totalScore <= 0.0) {
                totalScore = 0.01;
            }

            String snippet = snippetGenerator.generateSnippet(doc.getPreviewText(), queryTerms);
            String fullExplanation = explanations.isEmpty() ? "Boolean retrieval match" : String.join(" | ", explanations);

            rankedResults.add(new SearchResult(
                doc,
                Math.round(totalScore * 1000.0) / 1000.0,
                matchedTermsList,
                snippet,
                totalMatches,
                fullExplanation
            ));
        }

        Collections.sort(rankedResults);

        int totalFound = rankedResults.size();
        if (limit > 0 && rankedResults.size() > limit) {
            rankedResults = rankedResults.subList(0, limit);
        }

        double timeTakenMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return new SearchResponse(rawQuery, scorer.getName(), totalFound, Math.round(timeTakenMs * 100.0) / 100.0, rankedResults);
    }
}
