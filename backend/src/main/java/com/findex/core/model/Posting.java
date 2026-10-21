package com.findex.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Posting implements Serializable {
    private final int docId;
    private int termFrequency;
    private final List<Integer> positions;

    public Posting(int docId) {
        this.docId = docId;
        this.termFrequency = 0;
        this.positions = new ArrayList<>();
    }

    public Posting(int docId, int termFrequency, List<Integer> positions) {
        this.docId = docId;
        this.termFrequency = termFrequency;
        this.positions = new ArrayList<>(positions);
    }

    public void addPosition(int position) {
        positions.add(position);
        termFrequency++;
    }

    public int getDocId() {
        return docId;
    }

    public int getTermFrequency() {
        return termFrequency;
    }

    public List<Integer> getPositions() {
        return Collections.unmodifiableList(positions);
    }
}
