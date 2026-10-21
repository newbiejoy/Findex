package com.findex.core.model;

import java.io.Serializable;

public class IndexStats implements Serializable {
    private final int totalDocuments;
    private final int uniqueTerms;
    private final long totalPostings;
    private final double avgDocLength;
    private final double memoryUsageMb;
    private final long lastIndexedAt;

    public IndexStats(int totalDocuments, int uniqueTerms, long totalPostings,
                      double avgDocLength, double memoryUsageMb, long lastIndexedAt) {
        this.totalDocuments = totalDocuments;
        this.uniqueTerms = uniqueTerms;
        this.totalPostings = totalPostings;
        this.avgDocLength = avgDocLength;
        this.memoryUsageMb = memoryUsageMb;
        this.lastIndexedAt = lastIndexedAt;
    }

    public int getTotalDocuments() { return totalDocuments; }
    public int getUniqueTerms() { return uniqueTerms; }
    public long getTotalPostings() { return totalPostings; }
    public double getAvgDocLength() { return avgDocLength; }
    public double getMemoryUsageMb() { return memoryUsageMb; }
    public long getLastIndexedAt() { return lastIndexedAt; }
}
