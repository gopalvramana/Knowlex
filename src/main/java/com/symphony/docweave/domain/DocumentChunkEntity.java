package com.symphony.docweave.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
public class DocumentChunkEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @Column(nullable = false)
    private int chunkIndex;

    @Column(name = "chunk_text", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;

    protected DocumentChunkEntity() {} // JPA

    public DocumentChunkEntity(UUID id, DocumentEntity document, int chunkIndex, String content) {
        this.id = id;
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
    }
}
