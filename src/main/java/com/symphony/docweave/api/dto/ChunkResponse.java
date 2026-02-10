package com.symphony.docweave.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class ChunkResponse {

    private UUID id;
    private UUID documentId;
    private int chunkIndex;
    private String content;
}
