package com.symphony.docweave.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RagRequest {

    /** The question to answer */
    private String query;

    /**
     * How many chunks to retrieve for context (optional; falls back to
     * embedding.search.default-k from application.yaml)
     */
    private Integer topK;
}
