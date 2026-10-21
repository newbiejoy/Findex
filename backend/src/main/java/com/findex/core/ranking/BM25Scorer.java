package com.findex.core.ranking;

import com.findex.core.model.Posting;

public class BM25Scorer implements Scorer {
    private final double k1;
    private final double b;

    public BM25Scorer() {
        this(1.2, 0.75);
    }

    public BM25Scorer(double k1, double b) {
        this.k1 = k1;
        this.b = b;
    }

    @Override
    public String getName() {
        return "BM25";
    }

    @Override
    public double score(String term, Posting posting, int docLength, int totalDocs, int docFreq, double avgDocLength) {
        if (posting == null || posting.getTermFrequency() <= 0 || totalDocs <= 0 || docFreq <= 0) {
            return 0.0;
        }

        double idf = Math.log(1.0 + ((totalDocs - docFreq + 0.5) / (docFreq + 0.5)));
        if (idf < 0.0001) {
            idf = 0.0001;
        }

        double tf = posting.getTermFrequency();
        double effectiveAvgLen = Math.max(1.0, avgDocLength);
        double lenNorm = 1.0 - b + b * (docLength / effectiveAvgLen);
        double tfNorm = (tf * (k1 + 1.0)) / (tf + k1 * lenNorm);

        return idf * tfNorm;
    }

    @Override
    public String explain(String term, Posting posting, int docLength, int totalDocs, int docFreq, double avgDocLength) {
        if (posting == null || posting.getTermFrequency() <= 0) {
            return term + ": BM25 Score=0.00";
        }
        double idf = Math.log(1.0 + ((totalDocs - docFreq + 0.5) / (docFreq + 0.5)));
        if (idf < 0.0001) idf = 0.0001;

        double tf = posting.getTermFrequency();
        double effectiveAvgLen = Math.max(1.0, avgDocLength);
        double lenNorm = 1.0 - b + b * (docLength / effectiveAvgLen);
        double tfNorm = (tf * (k1 + 1.0)) / (tf + k1 * lenNorm);
        double score = idf * tfNorm;

        return String.format("%s: IDF=%.2f, TF_Norm=%.2f (tf=%d, len=%d, avgLen=%.1f) => BM25=%.3f",
            term, idf, tfNorm, posting.getTermFrequency(), docLength, effectiveAvgLen, score);
    }
}
