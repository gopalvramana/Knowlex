package com.symphony.docweave.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DocumentChunk {

    private final String chunkId;
    private final String documentId;
    private final int chunkIndex;
    private String content;
    private Map<String, Object> metadata = new HashMap<>();

    public DocumentChunk(String chunkId, String documentId, int chunkIndex, String content) {
        this.chunkId = UUID.randomUUID().toString();
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
    }

    public String getChunkId() {
        return chunkId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

}
