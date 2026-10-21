package com.findex.service;

import com.findex.core.extractor.DocumentExtractor;
import com.findex.core.index.InvertedIndex;
import com.findex.core.model.*;
import com.findex.core.tokenizer.StandardTokenizer;
import com.findex.storage.DatabaseManager;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IndexingService {
    private final InvertedIndex index = new InvertedIndex();
    private final DocumentExtractor extractor = new DocumentExtractor();
    private final StandardTokenizer tokenizer = new StandardTokenizer();
    private final DatabaseManager dbManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicInteger docIdGenerator = new AtomicInteger(1);

    private volatile IndexingProgress currentProgress = IndexingProgress.idle();

    public IndexingService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @PostConstruct
    public void init() {
        // Automatically register initial default "documents" folder if present in current dir
        File defaultDocs = new File("documents");
        if (defaultDocs.exists() && defaultDocs.isDirectory()) {
            try {
                String abs = defaultDocs.getCanonicalPath();
                if (dbManager.getFolders().stream().noneMatch(f -> f.getPath().equalsIgnoreCase(abs))) {
                    registerFolder(abs, "Documents");
                }
            } catch (Exception ignored) {}
        }

        // Restore documents and index on startup
        reindexAll();
    }

    public InvertedIndex getIndex() {
        return index;
    }

    public IndexingProgress getStatus() {
        return currentProgress;
    }

    public List<FolderConfig> getRegisteredFolders() {
        return dbManager.getFolders();
    }

    public FolderConfig registerFolder(String pathStr, String customName) throws IOException {
        Path path = Paths.get(pathStr).toRealPath();
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path is not a valid directory: " + pathStr);
        }

        String absPath = path.toString();
        String name = (customName != null && !customName.isBlank()) ? customName : path.getFileName().toString();
        String id = UUID.randomUUID().toString().substring(0, 8);

        FolderConfig folder = new FolderConfig(id, absPath, name, 0, System.currentTimeMillis(), 0);
        dbManager.saveFolder(folder);

        // Reindex asynchronously
        reindexFolder(id);

        return folder;
    }

    public void removeFolder(String folderId) {
        dbManager.deleteFolder(folderId);
        // Remove associated docs from index
        for (IndexedDocument doc : index.getAllDocuments()) {
            if (folderId.equals(doc.getFolderId())) {
                index.removeDocument(doc.getId());
            }
        }
    }

    public synchronized CompletableFuture<Void> reindexAll() {
        return CompletableFuture.runAsync(() -> {
            List<FolderConfig> folders = dbManager.getFolders();
            if (folders.isEmpty()) {
                currentProgress = IndexingProgress.idle();
                return;
            }

            index.clear();
            for (FolderConfig folder : folders) {
                indexFolderInternal(folder);
            }
            currentProgress = new IndexingProgress(
                IndexingProgress.Status.COMPLETED, 
                currentProgress.getTotalFiles(), 
                currentProgress.getIndexedFiles(), 
                "", 100.0, null, currentProgress.getDurationMs()
            );
        }, executor);
    }

    public synchronized CompletableFuture<Void> reindexFolder(String folderId) {
        return CompletableFuture.runAsync(() -> {
            Optional<FolderConfig> opt = dbManager.getFolder(folderId);
            if (opt.isEmpty()) return;

            FolderConfig folder = opt.get();
            // Clear prior docs for this folder from index & db
            for (IndexedDocument doc : index.getAllDocuments()) {
                if (folderId.equals(doc.getFolderId())) {
                    index.removeDocument(doc.getId());
                }
            }
            dbManager.clearFolderDocuments(folderId);

            indexFolderInternal(folder);
        }, executor);
    }

    private void indexFolderInternal(FolderConfig folder) {
        long startTime = System.currentTimeMillis();
        Path root = Paths.get(folder.getPath());
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            currentProgress = new IndexingProgress(
                IndexingProgress.Status.FAILED, 0, 0, "", 0, 
                "Directory does not exist: " + folder.getPath(), 0
            );
            return;
        }

        currentProgress = new IndexingProgress(
            IndexingProgress.Status.SCANNING, 0, 0, "Scanning files in " + folder.getName(), 0, null, 0
        );

        List<Path> candidateFiles = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (name.startsWith(".") || "node_modules".equalsIgnoreCase(name) 
                        || "target".equalsIgnoreCase(name) || "build".equalsIgnoreCase(name)
                        || "bin".equalsIgnoreCase(name) || ".git".equalsIgnoreCase(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (extractor.isSupported(file)) {
                        candidateFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error scanning folder: " + e.getMessage());
        }

        int total = candidateFiles.size();
        int indexedCount = 0;

        for (Path file : candidateFiles) {
            String fileName = file.getFileName().toString();
            double percent = total > 0 ? (indexedCount * 100.0 / total) : 0;
            currentProgress = new IndexingProgress(
                IndexingProgress.Status.INDEXING, total, indexedCount, fileName, 
                Math.round(percent * 10.0) / 10.0, null, System.currentTimeMillis() - startTime
            );

            try {
                String text = extractor.extractText(file);
                List<Token> tokens = tokenizer.tokenize(text);

                int docId = docIdGenerator.getAndIncrement();
                String ext = DocumentExtractor.getExtension(file);
                long size = Files.size(file);
                long lastMod = Files.getLastModifiedTime(file).toMillis();

                // Save preview text (up to 30KB for instant snippets and search inspection)
                String preview = text.length() > 30000 ? text.substring(0, 30000) : text;

                IndexedDocument doc = new IndexedDocument(
                    docId, folder.getId(), file.toAbsolutePath().toString(),
                    fileName, ext, size, lastMod, tokens.size(), preview
                );

                index.addDocument(doc, tokens);
                dbManager.saveDocument(doc);
                indexedCount++;
            } catch (Exception e) {
                System.err.println("Could not index file " + fileName + ": " + e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        FolderConfig updated = new FolderConfig(
            folder.getId(), folder.getPath(), folder.getName(), indexedCount, folder.getAddedAt(), System.currentTimeMillis()
        );
        dbManager.saveFolder(updated);

        currentProgress = new IndexingProgress(
            IndexingProgress.Status.COMPLETED, total, indexedCount, "", 100.0, null, duration
        );
    }
}
