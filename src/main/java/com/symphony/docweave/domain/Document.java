package com.symphony.docweave.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public class Document {

    private final String documentId;
    private final String sourceName;
    private final DocumentType documentType;
    private final Instant createdAt;

    public Document(String sourceName, DocumentType documentType) {
        this.documentId = UUID.randomUUID().toString();
        this.sourceName = sourceName;
        this.documentType = documentType;
        this.createdAt = Instant.now();
    }


}
