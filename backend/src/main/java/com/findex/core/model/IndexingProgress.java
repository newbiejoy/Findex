package com.findex.core.model;

import java.io.Serializable;

public class IndexingProgress implements Serializable {
    public enum Status { IDLE, SCANNING, INDEXING, COMPLETED, FAILED }

    private final Status status;
    private final int totalFiles;
    private final int indexedFiles;
    private final String currentFile;
    private final double percent;
    private final String errorMessage;
    private final long durationMs;

    public IndexingProgress(Status status, int totalFiles, int indexedFiles, 
                            String currentFile, double percent, String errorMessage, long durationMs) {
        this.status = status;
        this.totalFiles = totalFiles;
        this.indexedFiles = indexedFiles;
        this.currentFile = currentFile;
        this.percent = percent;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }

    public static IndexingProgress idle() {
        return new IndexingProgress(Status.IDLE, 0, 0, "", 100.0, null, 0);
    }

    public Status getStatus() { return status; }
    public int getTotalFiles() { return totalFiles; }
    public int getIndexedFiles() { return indexedFiles; }
    public String getCurrentFile() { return currentFile; }
    public double getPercent() { return percent; }
    public String getErrorMessage() { return errorMessage; }
    public long getDurationMs() { return durationMs; }
}
