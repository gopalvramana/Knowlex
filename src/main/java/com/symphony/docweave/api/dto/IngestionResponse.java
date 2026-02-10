package com.symphony.docweave.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class IngestionResponse {

    private UUID documentId;
    private String filename;
    private int totalChunks;
    private String status;
    private Instant createdAt;
}
