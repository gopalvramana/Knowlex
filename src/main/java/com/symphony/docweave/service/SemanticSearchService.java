package com.symphony.docweave.service;

import com.symphony.docweave.api.dto.SearchResult;
import com.symphony.docweave.repository.DocumentChunkRepository;
import com.symphony.docweave.util.OpenAiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);

    private final OpenAiEmbeddingClient embeddingClient;
    private final DocumentChunkRepository chunkRepository;

    @Value("${embedding.search.default-k:5}")
    private int defaultK;

    @Value("${embedding.search.max-k:50}")
    private int maxK;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Embed {@code query}, retrieve the top-K most similar chunks from the DB,
     * and return them as ranked {@link SearchResult}s (closest first).
     *
     * @param query raw user query string
     * @param k     number of results to return; clamped to [1, maxK]
     * @return ranked list of matching chunks with their similarity scores
     * @throws IllegalArgumentException if the query is blank
     */
    public List<SearchResult> search(String query, int k) {
        validateQuery(query);
        int clampedK = clamp(k, 1, maxK);

        log.info("Semantic search | k={} | query=\"{}\"", clampedK, query);

        // 1. Embed the query text into a vector
        float[] queryVector = embeddingClient.embed(List.of(query)).get(0);

        // 2. Convert float[] → PostgreSQL-compatible vector literal "[x,x,x,...]"
        String pgVector = toPgVectorLiteral(queryVector);

        // 3. Query DB for top-K nearest chunks
        List<Object[]> rows = chunkRepository.findTopKSimilar(pgVector, clampedK);

        // 4. Map raw rows → SearchResult DTOs
        List<SearchResult> results = rows.stream()
                .map(this::toSearchResult)
                .toList();

        log.info("Semantic search returned {} result(s)", results.size());
        return results;
    }

    /**
     * Overload using the configured default K.
     */
    public List<SearchResult> search(String query) {
        return search(query, defaultK);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    /**
     * Maps a raw Object[] row from the native query to a {@link SearchResult}.
     *
     * Column order matches the SELECT in DocumentChunkRepository:
     *   [0] id          UUID
     *   [1] document_id UUID
     *   [2] chunk_index int
     *   [3] chunk_text  String
     *   [4] embedding   (ignored here)
     *   [5] score       double (cosine distance)
     */
    private SearchResult toSearchResult(Object[] row) {
        UUID chunkId    = UUID.fromString(row[0].toString());
        UUID documentId = UUID.fromString(row[1].toString());
        int chunkIndex  = ((Number) row[2]).intValue();
        String content  = (String) row[3];
        double score    = ((Number) row[5]).doubleValue();

        return new SearchResult(chunkId, documentId, chunkIndex, content, score);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void validateQuery(String query) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("Search query must not be blank");
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * Converts a float[] to the PostgreSQL vector literal format: "[0.1,0.2,...]"
     *
     * IMPORTANT: must use plain decimal notation (%.8f), never scientific
     * notation (e.g. 1.23E-5), which pgvector's parser rejects.
     */
    private static String toPgVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(String.format("%.8f", vector[i]));
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
