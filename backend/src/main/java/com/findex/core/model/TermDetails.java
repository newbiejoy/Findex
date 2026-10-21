package com.findex.core.model;

import java.io.Serializable;
import java.util.List;

public class TermDetails implements Serializable {
    private final String term;
    private final int documentFrequency;
    private final int totalOccurrences;
    private final List<PostingSummary> samplePostings;

    public static class PostingSummary implements Serializable {
        private final int docId;
        private final String fileName;
        private final int termFrequency;
        private final List<Integer> positions;

        public PostingSummary(int docId, String fileName, int termFrequency, List<Integer> positions) {
            this.docId = docId;
            this.fileName = fileName;
            this.termFrequency = termFrequency;
            this.positions = positions;
        }

        public int getDocId() { return docId; }
        public String getFileName() { return fileName; }
        public int getTermFrequency() { return termFrequency; }
        public List<Integer> getPositions() { return positions; }
    }

    public TermDetails(String term, int documentFrequency, int totalOccurrences, List<PostingSummary> samplePostings) {
        this.term = term;
        this.documentFrequency = documentFrequency;
        this.totalOccurrences = totalOccurrences;
        this.samplePostings = samplePostings;
    }

    public String getTerm() { return term; }
    public int getDocumentFrequency() { return documentFrequency; }
    public int getTotalOccurrences() { return totalOccurrences; }
    public List<PostingSummary> getSamplePostings() { return samplePostings; }
}
