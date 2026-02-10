package com.symphony.docweave.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class DocumentResponse {

    private UUID id;
    private String source;
    private String originalFilename;
    private String checksum;
    private Instant createdAt;
}
