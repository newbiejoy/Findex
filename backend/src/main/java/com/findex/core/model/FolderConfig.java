package com.findex.core.model;

import java.io.Serializable;

public class FolderConfig implements Serializable {
    private final String id;
    private final String path;
    private final String name;
    private final int fileCount;
    private final long addedAt;
    private final long lastIndexedAt;

    public FolderConfig(String id, String path, String name, int fileCount, long addedAt, long lastIndexedAt) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.fileCount = fileCount;
        this.addedAt = addedAt;
        this.lastIndexedAt = lastIndexedAt;
    }

    public String getId() { return id; }
    public String getPath() { return path; }
    public String getName() { return name; }
    public int getFileCount() { return fileCount; }
    public long getAddedAt() { return addedAt; }
    public long getLastIndexedAt() { return lastIndexedAt; }
}
