package com.findex.core.ranking;

import com.findex.core.model.Posting;

public class TfIdfScorer implements Scorer {

    @Override
    public String getName() {
        return "TF-IDF";
    }

    @Override
    public double score(String term, Posting posting, int docLength, int totalDocs, int docFreq, double avgDocLength) {
        if (posting == null || posting.getTermFrequency() <= 0 || totalDocs <= 0 || docFreq <= 0) {
            return 0.0;
        }

        // Sublinear TF scaling: 1 + ln(tf)
        double tf = 1.0 + Math.log(posting.getTermFrequency());
        
        // Smooth Inverse Document Frequency: ln(1 + N / df)
        double idf = Math.log(1.0 + ((double) totalDocs / docFreq));

        return tf * idf;
    }

    @Override
    public String explain(String term, Posting posting, int docLength, int totalDocs, int docFreq, double avgDocLength) {
        if (posting == null || posting.getTermFrequency() <= 0) {
            return term + ": TF=0, Score=0.00";
        }
        double tf = 1.0 + Math.log(posting.getTermFrequency());
        double idf = Math.log(1.0 + ((double) totalDocs / Math.max(1, docFreq)));
        double score = tf * idf;
        return String.format("%s: TF=%.2f (count=%d), IDF=%.2f (df=%d/%d) => Score=%.3f",
            term, tf, posting.getTermFrequency(), idf, docFreq, totalDocs, score);
    }
}
