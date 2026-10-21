package com.findex.core.model;

import java.io.Serializable;
import java.util.List;

public class SearchResponse implements Serializable {
    private final String query;
    private final String mode;
    private final int totalResults;
    private final double timeTakenMs;
    private final List<SearchResult> results;

    public SearchResponse(String query, String mode, int totalResults, double timeTakenMs, List<SearchResult> results) {
        this.query = query;
        this.mode = mode;
        this.totalResults = totalResults;
        this.timeTakenMs = timeTakenMs;
        this.results = results;
    }

    public String getQuery() { return query; }
    public String getMode() { return mode; }
    public int getTotalResults() { return totalResults; }
    public double getTimeTakenMs() { return timeTakenMs; }
    public List<SearchResult> getResults() { return results; }
}
