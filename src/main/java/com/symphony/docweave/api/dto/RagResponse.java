package com.symphony.docweave.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RagResponse {

    /** The original user question */
    private String query;

    /** The LLM-generated answer */
    private String answer;

    /** The chunks that were used as context (for transparency / citation) */
    private List<SearchResult> sourceChunks;
}
