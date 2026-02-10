package com.symphony.docweave.chunker;

import com.symphony.docweave.config.IngestionProperties;
import com.symphony.docweave.domain.DocumentChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowChunkerTest {

    private SlidingWindowChunker chunker;

    @BeforeEach
    void setUp() {
        IngestionProperties properties = new IngestionProperties();
        properties.setChunkSize(5);
        properties.setChunkOverlap(2);
        chunker = new SlidingWindowChunker(properties);
    }

    @Test
    void chunk_shouldReturnSingleChunk_whenTextFitsInOneChunk() {
        String text = "one two three four five";
        String documentId = UUID.randomUUID().toString();

        List<DocumentChunk> chunks = chunker.chunk(text, documentId);

        assertEquals(1, chunks.size());
        assertEquals("one two three four five", chunks.get(0).getContent());
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertEquals(documentId, chunks.get(0).getDocumentId());
    }

    @Test
    void chunk_shouldCreateOverlappingChunks() {
        // 10 words, chunkSize=5, overlap=2 -> step=3
        // chunk0: words 0-4, chunk1: words 3-7, chunk2: words 6-9
        String text = "w1 w2 w3 w4 w5 w6 w7 w8 w9 w10";
        String documentId = UUID.randomUUID().toString();

        List<DocumentChunk> chunks = chunker.chunk(text, documentId);

        assertEquals(3, chunks.size());
        assertEquals("w1 w2 w3 w4 w5", chunks.get(0).getContent());
        assertEquals("w4 w5 w6 w7 w8", chunks.get(1).getContent());
        assertEquals("w7 w8 w9 w10", chunks.get(2).getContent());
    }

    @Test
    void chunk_shouldAssignSequentialChunkIndices() {
        String text = "a b c d e f g h i j k l m n o";
        String documentId = UUID.randomUUID().toString();

        List<DocumentChunk> chunks = chunker.chunk(text, documentId);

        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getChunkIndex());
        }
    }

    @Test
    void chunk_shouldSetMetadata() {
        String text = "one two three four five six seven eight";
        String documentId = UUID.randomUUID().toString();

        List<DocumentChunk> chunks = chunker.chunk(text, documentId);

        assertNotNull(chunks.get(0).getMetadata().get("startIndex"));
        assertNotNull(chunks.get(0).getMetadata().get("endIndex"));
        assertEquals(0, chunks.get(0).getMetadata().get("startIndex"));
    }

    @Test
    void chunk_shouldHandleSingleWord() {
        String text = "hello";
        String documentId = UUID.randomUUID().toString();

        List<DocumentChunk> chunks = chunker.chunk(text, documentId);

        assertEquals(1, chunks.size());
        assertEquals("hello", chunks.get(0).getContent());
    }

    @Test
    void chunk_shouldAssignUniqueChunkIds() {
        String text = "a b c d e f g h i j";
        String documentId = UUID.randomUUID().toString();

        List<DocumentChunk> chunks = chunker.chunk(text, documentId);

        long uniqueIds = chunks.stream()
                .map(DocumentChunk::getChunkId)
                .distinct()
                .count();
        assertEquals(chunks.size(), uniqueIds);
    }

    @Test
    void chunk_shouldHandleLargeText() {
        // 200 words with default config (chunkSize=5, overlap=2, step=3)
        String text = IntStream.range(0, 200)
                .mapToObj(i -> "word" + i)
                .collect(Collectors.joining(" "));
        String documentId = UUID.randomUUID().toString();

        List<DocumentChunk> chunks = chunker.chunk(text, documentId);

        assertTrue(chunks.size() > 1);
        // First chunk should have 5 words
        assertEquals(5, chunks.get(0).getContent().split(" ").length);
    }

    @Test
    void chunk_shouldHandleMultipleWhitespace() {
        String text = "one   two  three    four  five  six  seven";
        String documentId = UUID.randomUUID().toString();

        List<DocumentChunk> chunks = chunker.chunk(text, documentId);

        // split("\\s+") handles multiple whitespace
        assertFalse(chunks.isEmpty());
        assertFalse(chunks.get(0).getContent().contains("  "));
    }

    @Test
    void chunk_shouldUseConfiguredProperties() {
        IngestionProperties customProps = new IngestionProperties();
        customProps.setChunkSize(3);
        customProps.setChunkOverlap(1);
        SlidingWindowChunker customChunker = new SlidingWindowChunker(customProps);

        // 6 words, chunkSize=3, overlap=1 -> step=2
        // chunk0: words 0-2, chunk1: words 2-4, chunk2: words 4-5
        String text = "a b c d e f";
        List<DocumentChunk> chunks = customChunker.chunk(text, "doc-1");

        assertEquals(3, chunks.size());
        assertEquals("a b c", chunks.get(0).getContent());
        assertEquals("c d e", chunks.get(1).getContent());
        assertEquals("e f", chunks.get(2).getContent());
    }
}
