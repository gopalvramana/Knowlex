package com.symphony.docweave.chunker;

import com.symphony.docweave.domain.DocumentChunk;

import java.util.List;

public interface TextChunker {
    List<DocumentChunk> chunk(String text, String documentId);
}
