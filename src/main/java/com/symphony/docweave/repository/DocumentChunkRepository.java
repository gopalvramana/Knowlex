package com.symphony.docweave.repository;

import com.symphony.docweave.domain.DocumentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, UUID> {

    List<DocumentChunkEntity> findByDocumentIdOrderByChunkIndex(UUID documentId);

    @Modifying
    @Transactional
    void deleteByDocumentId(UUID documentId);

    /**
     * Fetches only chunks without an embedding.
     * Uses JPQL (not findAll) to avoid Hibernate trying to hydrate the
     * pgvector column for every row, which causes PSQLException.
     */
    @Query("SELECT c FROM DocumentChunkEntity c WHERE c.embedding IS NULL")
    List<DocumentChunkEntity> findAllWithoutEmbedding();

    /**
     * Same as above but scoped to a single document.
     */
    @Query("SELECT c FROM DocumentChunkEntity c WHERE c.document.id = :documentId AND c.embedding IS NULL ORDER BY c.chunkIndex")
    List<DocumentChunkEntity> findByDocumentIdWithoutEmbedding(@Param("documentId") UUID documentId);

    @Query(value = """
        SELECT id, document_id, chunk_index, chunk_text, embedding,
               embedding <=> CAST(:query AS vector) AS score
        FROM document_chunks
        WHERE embedding IS NOT NULL
        ORDER BY score
        LIMIT :k
        """, nativeQuery = true)
    List<Object[]> findTopKSimilar(@Param("query") String query, @Param("k") int k);
}
