package com.symphony.docweave.chunker;

import com.symphony.docweave.domain.DocumentChunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SlidingWindowChunker implements TextChunker{

    // The size of each chunk
    private final int chunkSize;
    // The number of characters to overlap between chunks
    private final int overlap;

    public SlidingWindowChunker(int chunkSize, int overlap) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }


    @Override
    public List<DocumentChunk> chunk(String text, String documentId) {

        String[] words = text.split(" ");
        List<DocumentChunk> chunks = new ArrayList<>();

        int chunkIndex = 0;
        int step = chunkSize - overlap;

        // Loop through the text and create chunks with the specified overlap
        for (int i = 0; i < words.length; i += step) {

            int end = Math.min(i + chunkSize, words.length);
            String chunkText = String.join(" ",
                    Arrays.copyOfRange(words, i, end));

            DocumentChunk chunk = new DocumentChunk(
                    UUID.randomUUID().toString(), // chunkId
                    documentId, // documentId
                    chunkIndex++, // chunkIndex
                    chunkText
            );
            // Add metadata for the chunk
            chunk.setMetadata("startIndex", i);
            chunk.setMetadata("endIndex", end);
            // Add the chunk to the list of chunks
            chunks.add(chunk);

            if (end == words.length) break;
        }
        return chunks;
    }
}
