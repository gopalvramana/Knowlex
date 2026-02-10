package com.symphony.docweave.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
public class DocumentEntity {

    @Id
    private UUID id;

    private String source;

    private String originalFilename;

    private String checksum;

    private Instant createdAt;

    protected DocumentEntity() {} // JPA

    public DocumentEntity(UUID id, String source, String originalFilename, String checksum) {
        this.id = id;
        this.source = source;
        this.originalFilename = originalFilename;
        this.checksum = checksum;
        this.createdAt = Instant.now();
    }
}
