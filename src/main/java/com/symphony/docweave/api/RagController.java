package com.symphony.docweave.api;

import com.symphony.docweave.api.dto.RagRequest;
import com.symphony.docweave.api.dto.RagResponse;
import com.symphony.docweave.service.RagService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final RagService ragService;

    /**
     * POST /api/v1/rag/ask
     *
     * Request body:
     * {
     *   "query": "What is the refund policy?",
     *   "topK": 5          // optional, defaults to rag.default-top-k
     * }
     *
     * Response:
     * {
     *   "query": "What is the refund policy?",
     *   "answer": "According to the documents, ...",
     *   "sourceChunks": [ { "chunkId": "...", "documentId": "...", "chunkIndex": 2,
     *                        "content": "...", "score": 0.12 }, ... ]
     * }
     */
    @PostMapping("/ask")
    public ResponseEntity<RagResponse> ask(@RequestBody RagRequest request) {
        log.info("RAG request received: query=\"{}\" topK={}", request.getQuery(), request.getTopK());
        RagResponse response = ragService.answer(request);
        return ResponseEntity.ok(response);
    }
}
