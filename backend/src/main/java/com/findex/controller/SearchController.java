package com.findex.controller;

import com.findex.core.model.IndexedDocument;
import com.findex.core.model.SearchResponse;
import com.findex.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "mode", defaultValue = "bm25") String mode,
            @RequestParam(name = "type", required = false) String fileType,
            @RequestParam(name = "folderId", required = false) String folderId,
            @RequestParam(name = "limit", defaultValue = "30") int limit) {
        return ResponseEntity.ok(searchService.search(query, mode, fileType, folderId, limit));
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<?> getDocument(@PathVariable("id") int id) {
        Optional<IndexedDocument> doc = searchService.getDocument(id);
        if (doc.isPresent()) {
            return ResponseEntity.ok(doc.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/documents/{id}/open")
    public ResponseEntity<?> openDocumentInOs(@PathVariable("id") int id) {
        Optional<IndexedDocument> doc = searchService.getDocument(id);
        if (doc.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            File file = new File(doc.get().getFilePath());
            if (!file.exists()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File does not exist on disk"));
            }

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
                return ResponseEntity.ok(Map.of("status", "opened", "path", file.getAbsolutePath()));
            } else {

                new ProcessBuilder("cmd.exe", "/c", "start", "", file.getAbsolutePath()).start();
                return ResponseEntity.ok(Map.of("status", "launched", "path", file.getAbsolutePath()));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam(name = "q", defaultValue = "") String prefix,
            @RequestParam(name = "limit", defaultValue = "8") int limit) {
        return ResponseEntity.ok(searchService.getSuggestions(prefix, limit));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.getHistory(limit));
    }

    @DeleteMapping("/history")
    public ResponseEntity<?> clearHistory() {
        searchService.clearHistory();
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }
}
