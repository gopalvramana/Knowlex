package com.symphony.docweave.api;

import com.symphony.docweave.api.dto.SearchResult;
import com.symphony.docweave.repository.DocumentChunkRepository;
import com.symphony.docweave.service.EmbeddingService;
import com.symphony.docweave.service.SemanticSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/embeddings")
public class EmbeddingController {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingController.class);

    private final EmbeddingService embeddingService;
    private final SemanticSearchService searchService;
    private final DocumentChunkRepository chunkRepository;

    public EmbeddingController(EmbeddingService embeddingService,
                               SemanticSearchService searchService,
                               DocumentChunkRepository chunkRepository) {
        this.embeddingService = embeddingService;
        this.searchService = searchService;
        this.chunkRepository = chunkRepository;
    }

    /**
     * POST /api/v1/embeddings/generate
     * Embed all chunks (across all documents) that don't have an embedding yet.
     */
    @PostMapping("/generate")
    public ResponseEntity<String> generateAll() {
        log.info("Embedding all un-embedded chunks");
        int count = embeddingService.generateEmbeddingsForAllChunks();
        return ResponseEntity.ok("Embedded " + count + " chunk(s)");
    }

    /**
     * POST /api/v1/embeddings/generate/{documentId}
     * Embed all un-embedded chunks for a specific document.
     */
    @PostMapping("/generate/{documentId}")
    public ResponseEntity<String> generateForDocument(@PathVariable UUID documentId) {
        log.info("Embedding chunks for document {}", documentId);
        int count = embeddingService.generateEmbeddingsForDocument(documentId);
        if (count == 0 && chunkRepository.findByDocumentIdOrderByChunkIndex(documentId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Embedded " + count + " chunk(s) for document " + documentId);
    }

    /**
     * POST /api/v1/embeddings/search?k=5
     * Body: plain-text query string.
     * Returns the top-k most similar chunks with their similarity scores.
     */
    @PostMapping("/search")
    public ResponseEntity<List<SearchResult>> search(
            @RequestBody String query,
            @RequestParam(defaultValue = "5") int k) {

        return ResponseEntity.ok(searchService.search(query, k));
    }
}
