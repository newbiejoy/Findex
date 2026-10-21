package com.findex.controller;

import com.findex.storage.DatabaseManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SettingsController {
    private final DatabaseManager dbManager;

    public SettingsController(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getSettings() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("defaultMode", dbManager.getSetting("defaultMode", "bm25"));
        map.put("snippetLength", dbManager.getSetting("snippetLength", "220"));
        map.put("theme", dbManager.getSetting("theme", "dark"));
        map.put("autoReindexOnStart", dbManager.getSetting("autoReindexOnStart", "true"));
        return ResponseEntity.ok(map);
    }

    @PostMapping("/settings")
    public ResponseEntity<?> saveSettings(@RequestBody Map<String, String> payload) {
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            dbManager.saveSetting(entry.getKey(), entry.getValue());
        }
        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "version", "1.0.0",
            "service", "Findex Engine",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
