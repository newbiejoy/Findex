package com.findex.core.model;

import java.io.Serializable;

public class IndexedDocument implements Serializable {
    private final int id;
    private final String folderId;
    private final String filePath;
    private final String fileName;
    private final String extension;
    private final long fileSize;
    private final long lastModified;
    private final int termCount;
    private final String previewText;

    public IndexedDocument(int id, String folderId, String filePath, String fileName,
                           String extension, long fileSize, long lastModified,
                           int termCount, String previewText) {
        this.id = id;
        this.folderId = folderId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.extension = extension;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.termCount = termCount;
        this.previewText = previewText;
    }

    public int getId() { return id; }
    public String getFolderId() { return folderId; }
    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public String getExtension() { return extension; }
    public long getFileSize() { return fileSize; }
    public long getLastModified() { return lastModified; }
    public int getTermCount() { return termCount; }
    public String getPreviewText() { return previewText; }
}
