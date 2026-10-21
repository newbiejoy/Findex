package com.findex.storage;

import com.findex.core.model.FolderConfig;
import com.findex.core.model.IndexedDocument;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.sql.*;
import java.util.*;

@Component
public class DatabaseManager {
    private static final String DB_FILE = "findex.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    @PostConstruct
    public void init() {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS registered_folders (
                        id TEXT PRIMARY KEY,
                        path TEXT NOT NULL UNIQUE,
                        name TEXT NOT NULL,
                        file_count INTEGER DEFAULT 0,
                        added_at INTEGER,
                        last_indexed_at INTEGER
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS documents (
                        id INTEGER PRIMARY KEY,
                        folder_id TEXT,
                        file_path TEXT NOT NULL UNIQUE,
                        file_name TEXT NOT NULL,
                        extension TEXT,
                        file_size INTEGER,
                        last_modified INTEGER,
                        term_count INTEGER,
                        preview_text TEXT,
                        indexed_at INTEGER
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS search_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        query TEXT NOT NULL,
                        mode TEXT,
                        result_count INTEGER,
                        latency_ms REAL,
                        searched_at INTEGER
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS settings (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    );
                """);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite database: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // Folders
    public List<FolderConfig> getFolders() {
        List<FolderConfig> list = new ArrayList<>();
        String sql = "SELECT id, path, name, file_count, added_at, last_indexed_at FROM registered_folders ORDER BY added_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new FolderConfig(
                    rs.getString("id"),
                    rs.getString("path"),
                    rs.getString("name"),
                    rs.getInt("file_count"),
                    rs.getLong("added_at"),
                    rs.getLong("last_indexed_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error reading folders: " + e.getMessage());
        }
        return list;
    }

    public Optional<FolderConfig> getFolder(String id) {
        String sql = "SELECT id, path, name, file_count, added_at, last_indexed_at FROM registered_folders WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new FolderConfig(
                        rs.getString("id"),
                        rs.getString("path"),
                        rs.getString("name"),
                        rs.getInt("file_count"),
                        rs.getLong("added_at"),
                        rs.getLong("last_indexed_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading folder: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void saveFolder(FolderConfig folder) {
        String sql = """
            INSERT INTO registered_folders (id, path, name, file_count, added_at, last_indexed_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                path = excluded.path,
                name = excluded.name,
                file_count = excluded.file_count,
                last_indexed_at = excluded.last_indexed_at;
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, folder.getId());
            ps.setString(2, folder.getPath());
            ps.setString(3, folder.getName());
            ps.setInt(4, folder.getFileCount());
            ps.setLong(5, folder.getAddedAt());
            ps.setLong(6, folder.getLastIndexedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save folder: " + e.getMessage(), e);
        }
    }

    public void deleteFolder(String id) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM documents WHERE folder_id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM registered_folders WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete folder: " + e.getMessage(), e);
        }
    }

    // Documents
    public void saveDocument(IndexedDocument doc) {
        String sql = """
            INSERT INTO documents (id, folder_id, file_path, file_name, extension, file_size, last_modified, term_count, preview_text, indexed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(file_path) DO UPDATE SET
                id = excluded.id,
                file_name = excluded.file_name,
                extension = excluded.extension,
                file_size = excluded.file_size,
                last_modified = excluded.last_modified,
                term_count = excluded.term_count,
                preview_text = excluded.preview_text,
                indexed_at = excluded.indexed_at;
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doc.getId());
            ps.setString(2, doc.getFolderId());
            ps.setString(3, doc.getFilePath());
            ps.setString(4, doc.getFileName());
            ps.setString(5, doc.getExtension());
            ps.setLong(6, doc.getFileSize());
            ps.setLong(7, doc.getLastModified());
            ps.setInt(8, doc.getTermCount());
            ps.setString(9, doc.getPreviewText());
            ps.setLong(10, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving document to SQLite: " + e.getMessage());
        }
    }

    public List<IndexedDocument> getAllDocuments() {
        List<IndexedDocument> list = new ArrayList<>();
        String sql = "SELECT id, folder_id, file_path, file_name, extension, file_size, last_modified, term_count, preview_text FROM documents";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new IndexedDocument(
                    rs.getInt("id"),
                    rs.getString("folder_id"),
                    rs.getString("file_path"),
                    rs.getString("file_name"),
                    rs.getString("extension"),
                    rs.getLong("file_size"),
                    rs.getLong("last_modified"),
                    rs.getInt("term_count"),
                    rs.getString("preview_text")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading documents: " + e.getMessage());
        }
        return list;
    }

    public void clearFolderDocuments(String folderId) {
        String sql = "DELETE FROM documents WHERE folder_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, folderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error clearing folder documents: " + e.getMessage());
        }
    }

    // Search History
    public void recordSearch(String query, String mode, int resultCount, double latencyMs) {
        String sql = "INSERT INTO search_history (query, mode, result_count, latency_ms, searched_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, query);
            ps.setString(2, mode);
            ps.setInt(3, resultCount);
            ps.setDouble(4, latencyMs);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error recording search: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getSearchHistory(int limit) {
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT id, query, mode, result_count, latency_ms, searched_at FROM search_history ORDER BY searched_at DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("query", rs.getString("query"));
                    map.put("mode", rs.getString("mode"));
                    map.put("resultCount", rs.getInt("result_count"));
                    map.put("latencyMs", rs.getDouble("latency_ms"));
                    map.put("searchedAt", rs.getLong("searched_at"));
                    history.add(map);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching search history: " + e.getMessage());
        }
        return history;
    }

    public void clearHistory() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM search_history");
        } catch (SQLException e) {
            System.err.println("Error clearing history: " + e.getMessage());
        }
    }

    // Settings
    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading setting: " + e.getMessage());
        }
        return defaultValue;
    }

    public void saveSetting(String key, String value) {
        String sql = "INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving setting: " + e.getMessage());
        }
    }
}
