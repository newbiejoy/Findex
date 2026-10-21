package com.findex.core.index;

import com.findex.core.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InvertedIndex {
    private final Map<String, List<Posting>> index = new ConcurrentHashMap<>();
    private final Map<Integer, IndexedDocument> documents = new ConcurrentHashMap<>();
    private final Map<String, Integer> pathIdMap = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private long totalTermLength = 0;
    private long totalPostingsCount = 0;
    private long lastIndexedTime = 0;

    public void addDocument(IndexedDocument doc, List<Token> tokens) {
        rwLock.writeLock().lock();
        try {

            if (documents.containsKey(doc.getId())) {
                removeDocument(doc.getId());
            }

            documents.put(doc.getId(), doc);
            pathIdMap.put(doc.getFilePath(), doc.getId());
            totalTermLength += tokens.size();

            Map<String, List<Integer>> termPositions = new HashMap<>();
            for (Token token : tokens) {
                termPositions.computeIfAbsent(token.getText(), k -> new ArrayList<>()).add(token.getPosition());
            }

            for (Map.Entry<String, List<Integer>> entry : termPositions.entrySet()) {
                String term = entry.getKey();
                List<Integer> positions = entry.getValue();
                Posting posting = new Posting(doc.getId(), positions.size(), positions);

                List<Posting> postingsList = index.computeIfAbsent(term, k -> new ArrayList<>());
                postingsList.add(posting);
                totalPostingsCount++;
            }

            lastIndexedTime = System.currentTimeMillis();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void removeDocument(int docId) {
        rwLock.writeLock().lock();
        try {
            IndexedDocument removed = documents.remove(docId);
            if (removed != null) {
                pathIdMap.remove(removed.getFilePath());
                totalTermLength = Math.max(0, totalTermLength - removed.getTermCount());

                Iterator<Map.Entry<String, List<Posting>>> it = index.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, List<Posting>> entry = it.next();
                    List<Posting> postings = entry.getValue();
                    boolean removedPosting = postings.removeIf(p -> p.getDocId() == docId);
                    if (removedPosting) {
                        totalPostingsCount = Math.max(0, totalPostingsCount - 1);
                    }
                    if (postings.isEmpty()) {
                        it.remove();
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void clear() {
        rwLock.writeLock().lock();
        try {
            index.clear();
            documents.clear();
            pathIdMap.clear();
            totalTermLength = 0;
            totalPostingsCount = 0;
            lastIndexedTime = System.currentTimeMillis();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public List<Posting> getPostings(String term) {
        rwLock.readLock().lock();
        try {
            List<Posting> list = index.get(term.toLowerCase(Locale.ROOT));
            if (list == null) return Collections.emptyList();
            return Collections.unmodifiableList(new ArrayList<>(list));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Posting getPosting(String term, int docId) {
        rwLock.readLock().lock();
        try {
            List<Posting> list = index.get(term.toLowerCase(Locale.ROOT));
            if (list == null) return null;
            for (Posting p : list) {
                if (p.getDocId() == docId) return p;
            }
            return null;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getDocumentFrequency(String term) {
        rwLock.readLock().lock();
        try {
            List<Posting> list = index.get(term.toLowerCase(Locale.ROOT));
            return list == null ? 0 : list.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public IndexedDocument getDocument(int docId) {
        return documents.get(docId);
    }

    public Optional<IndexedDocument> getDocumentByPath(String path) {
        Integer id = pathIdMap.get(path);
        if (id != null) {
            return Optional.ofNullable(documents.get(id));
        }
        return Optional.empty();
    }

    public Collection<IndexedDocument> getAllDocuments() {
        return Collections.unmodifiableCollection(documents.values());
    }

    public int getTotalDocuments() {
        return documents.size();
    }

    public double getAvgDocLength() {
        int count = documents.size();
        return count == 0 ? 0.0 : (double) totalTermLength / count;
    }

    public IndexStats getStats() {
        rwLock.readLock().lock();
        try {
            Runtime runtime = Runtime.getRuntime();
            double memoryUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
            return new IndexStats(
                documents.size(),
                index.size(),
                totalPostingsCount,
                getAvgDocLength(),
                Math.round(memoryUsedMb * 100.0) / 100.0,
                lastIndexedTime
            );
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<TermDetails> getTerms(String prefix, int offset, int limit) {
        rwLock.readLock().lock();
        try {
            List<String> matchingTerms = new ArrayList<>();
            String lowerPrefix = (prefix != null) ? prefix.toLowerCase(Locale.ROOT) : "";

            for (String term : index.keySet()) {
                if (lowerPrefix.isEmpty() || term.startsWith(lowerPrefix)) {
                    matchingTerms.add(term);
                }
            }

            Collections.sort(matchingTerms);

            int start = Math.min(offset, matchingTerms.size());
            int end = Math.min(start + limit, matchingTerms.size());
            List<String> paged = matchingTerms.subList(start, end);

            List<TermDetails> results = new ArrayList<>();
            for (String term : paged) {
                results.add(buildTermDetails(term));
            }
            return results;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public TermDetails getTermDetails(String term) {
        rwLock.readLock().lock();
        try {
            return buildTermDetails(term.toLowerCase(Locale.ROOT));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private TermDetails buildTermDetails(String term) {
        List<Posting> postings = index.get(term);
        if (postings == null) {
            return new TermDetails(term, 0, 0, Collections.emptyList());
        }

        int totalOccurrences = 0;
        List<TermDetails.PostingSummary> summaries = new ArrayList<>();
        for (Posting p : postings) {
            totalOccurrences += p.getTermFrequency();
            IndexedDocument doc = documents.get(p.getDocId());
            String fileName = (doc != null) ? doc.getFileName() : ("Doc #" + p.getDocId());
            summaries.add(new TermDetails.PostingSummary(
                p.getDocId(), fileName, p.getTermFrequency(), p.getPositions()
            ));
        }

        return new TermDetails(term, postings.size(), totalOccurrences, summaries);
    }
}
