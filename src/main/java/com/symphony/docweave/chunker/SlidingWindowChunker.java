package com.symphony.docweave.chunker;

import com.symphony.docweave.config.IngestionProperties;
import com.symphony.docweave.domain.DocumentChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class SlidingWindowChunker implements TextChunker {

    private final int chunkSize;
    private final int overlap;

    public SlidingWindowChunker(IngestionProperties properties) {
        this.chunkSize = properties.getChunkSize();
        this.overlap = properties.getChunkOverlap();
    }

    @Override
    public List<DocumentChunk> chunk(String text, String documentId) {

        String[] words = text.split("\\s+");
        List<DocumentChunk> chunks = new ArrayList<>();

        int chunkIndex = 0;
        int step = chunkSize - overlap;

        for (int i = 0; i < words.length; i += step) {

            int end = Math.min(i + chunkSize, words.length);
            String chunkText = String.join(" ",
                    Arrays.copyOfRange(words, i, end));

            DocumentChunk chunk = new DocumentChunk(
                    UUID.randomUUID().toString(),
                    documentId,
                    chunkIndex++,
                    chunkText
            );
            chunk.setMetadata("startIndex", i);
            chunk.setMetadata("endIndex", end);
            chunks.add(chunk);

            if (end == words.length) break;
        }
        return chunks;
    }
}
