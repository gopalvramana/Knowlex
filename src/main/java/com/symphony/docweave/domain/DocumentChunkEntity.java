package com.symphony.docweave.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Getter
public class DocumentChunkEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @Column(nullable = false)
    private int chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    protected DocumentChunkEntity() {} // JPA

    public DocumentChunkEntity(UUID id, DocumentEntity document, int chunkIndex, String content) {
        this.id = id;
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
    }
}
