package com.findex.core.model;

import java.io.Serializable;
import java.util.List;

public class SearchResult implements Serializable, Comparable<SearchResult> {
    private final IndexedDocument document;
    private final double score;
    private final List<String> matchedTerms;
    private final String snippet;
    private final int matchCount;
    private final String scoreExplanation;

    public SearchResult(IndexedDocument document, double score, List<String> matchedTerms, 
                        String snippet, int matchCount, String scoreExplanation) {
        this.document = document;
        this.score = score;
        this.matchedTerms = matchedTerms;
        this.snippet = snippet;
        this.matchCount = matchCount;
        this.scoreExplanation = scoreExplanation;
    }

    public IndexedDocument getDocument() { return document; }
    public double getScore() { return score; }
    public List<String> getMatchedTerms() { return matchedTerms; }
    public String getSnippet() { return snippet; }
    public int getMatchCount() { return matchCount; }
    public String getScoreExplanation() { return scoreExplanation; }

    @Override
    public int compareTo(SearchResult o) {
        return Double.compare(o.score, this.score); // descending by score
    }
}
