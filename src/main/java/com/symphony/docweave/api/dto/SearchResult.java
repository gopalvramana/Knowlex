package com.symphony.docweave.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class SearchResult {

    /** Chunk identity */
    private UUID chunkId;
    private UUID documentId;
    private int chunkIndex;

    /** The matched text */
    private String content;

    /** Cosine distance from the query (0 = identical, 1 = orthogonal) */
    private double score;
}
