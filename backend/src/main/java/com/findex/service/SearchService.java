package com.findex.service;

import com.findex.core.model.IndexedDocument;
import com.findex.core.model.SearchResponse;
import com.findex.core.model.TermDetails;
import com.findex.core.retrieval.SearchEngine;
import com.findex.storage.DatabaseManager;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchService {
    private final IndexingService indexingService;
    private final DatabaseManager dbManager;
    private final SearchEngine searchEngine;

    public SearchService(IndexingService indexingService, DatabaseManager dbManager) {
        this.indexingService = indexingService;
        this.dbManager = dbManager;
        this.searchEngine = new SearchEngine(indexingService.getIndex());
    }

    public SearchResponse search(String query, String mode, String fileType, String folderId, int limit) {
        SearchResponse response = searchEngine.search(query, mode, fileType, folderId, limit);

        // Record non-empty searches into database history
        if (query != null && !query.isBlank()) {
            dbManager.recordSearch(query, response.getMode(), response.getTotalResults(), response.getTimeTakenMs());
        }

        return response;
    }

    public Optional<IndexedDocument> getDocument(int id) {
        return Optional.ofNullable(indexingService.getIndex().getDocument(id));
    }

    public List<String> getSuggestions(String prefix, int limit) {
        List<String> suggestions = new ArrayList<>();
        if (prefix == null || prefix.isBlank()) return suggestions;

        List<TermDetails> terms = indexingService.getIndex().getTerms(prefix.trim().toLowerCase(Locale.ROOT), 0, limit);
        for (TermDetails t : terms) {
            suggestions.add(t.getTerm());
        }
        return suggestions;
    }

    public List<Map<String, Object>> getHistory(int limit) {
        return dbManager.getSearchHistory(limit);
    }

    public void clearHistory() {
        dbManager.clearHistory();
    }
}
