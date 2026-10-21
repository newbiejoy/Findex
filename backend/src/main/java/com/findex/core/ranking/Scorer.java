package com.findex.core.ranking;

import com.findex.core.model.Posting;

public interface Scorer {
    String getName();
    double score(String term, Posting posting, int docLength, int totalDocs, int docFreq, double avgDocLength);
    String explain(String term, Posting posting, int docLength, int totalDocs, int docFreq, double avgDocLength);
}
