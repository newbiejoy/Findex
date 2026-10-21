package com.findex.controller;

import com.findex.core.model.FolderConfig;
import com.findex.core.model.IndexStats;
import com.findex.core.model.IndexingProgress;
import com.findex.core.model.TermDetails;
import com.findex.service.IndexingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class IndexController {
    private final IndexingService indexingService;

    public IndexController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @GetMapping("/folders")
    public ResponseEntity<List<FolderConfig>> getFolders() {
        return ResponseEntity.ok(indexingService.getRegisteredFolders());
    }

    @PostMapping("/folders")
    public ResponseEntity<?> registerFolder(@RequestBody Map<String, String> payload) {
        String path = payload.get("path");
        String name = payload.get("name");

        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Folder path is required"));
        }

        try {
            FolderConfig folder = indexingService.registerFolder(path.trim(), name);
            return ResponseEntity.ok(folder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/folders/{id}")
    public ResponseEntity<?> deleteFolder(@PathVariable("id") String id) {
        indexingService.removeFolder(id);
        return ResponseEntity.ok(Map.of("status", "removed"));
    }

    @PostMapping("/folders/{id}/reindex")
    public ResponseEntity<?> reindexFolder(@PathVariable("id") String id) {
        indexingService.reindexFolder(id);
        return ResponseEntity.ok(Map.of("status", "indexing_started"));
    }

    @PostMapping("/index/rebuild")
    public ResponseEntity<?> rebuildIndex() {
        indexingService.reindexAll();
        return ResponseEntity.ok(Map.of("status", "reindex_all_started"));
    }

    @GetMapping("/index/status")
    public ResponseEntity<IndexingProgress> getStatus() {
        return ResponseEntity.ok(indexingService.getStatus());
    }

    @GetMapping("/stats")
    public ResponseEntity<IndexStats> getStats() {
        return ResponseEntity.ok(indexingService.getIndex().getStats());
    }

    @GetMapping("/terms")
    public ResponseEntity<List<TermDetails>> getTerms(
            @RequestParam(name = "prefix", defaultValue = "") String prefix,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(indexingService.getIndex().getTerms(prefix, offset, limit));
    }

    @GetMapping("/terms/{term}")
    public ResponseEntity<TermDetails> getTermDetails(@PathVariable("term") String term) {
        return ResponseEntity.ok(indexingService.getIndex().getTermDetails(term));
    }
}
