package com.findex.core.model;

import java.io.Serializable;
import java.util.List;

public class BenchmarkResult implements Serializable {
    private final String query;
    private final double linearScanMs;
    private final double invertedIndexMs;
    private final double speedupFactor;
    private final int matchingDocuments;
    private final int totalDocumentsScanned;
    private final List<SearchResult> topResults;

    public BenchmarkResult(String query, double linearScanMs, double invertedIndexMs, 
                           double speedupFactor, int matchingDocuments, 
                           int totalDocumentsScanned, List<SearchResult> topResults) {
        this.query = query;
        this.linearScanMs = linearScanMs;
        this.invertedIndexMs = invertedIndexMs;
        this.speedupFactor = speedupFactor;
        this.matchingDocuments = matchingDocuments;
        this.totalDocumentsScanned = totalDocumentsScanned;
        this.topResults = topResults;
    }

    public String getQuery() { return query; }
    public double getLinearScanMs() { return linearScanMs; }
    public double getInvertedIndexMs() { return invertedIndexMs; }
    public double getSpeedupFactor() { return speedupFactor; }
    public int getMatchingDocuments() { return matchingDocuments; }
    public int getTotalDocumentsScanned() { return totalDocumentsScanned; }
    public List<SearchResult> getTopResults() { return topResults; }
}
