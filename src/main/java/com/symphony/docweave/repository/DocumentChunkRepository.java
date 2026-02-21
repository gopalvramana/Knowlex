package com.symphony.docweave.repository;

import com.symphony.docweave.domain.DocumentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, UUID> {

    List<DocumentChunkEntity> findByDocumentIdOrderByChunkIndex(UUID documentId);

    void deleteByDocumentId(UUID documentId);

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
