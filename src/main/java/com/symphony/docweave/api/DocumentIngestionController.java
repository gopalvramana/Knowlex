package com.symphony.docweave.api;

import com.symphony.docweave.api.dto.ChunkResponse;
import com.symphony.docweave.api.dto.DocumentResponse;
import com.symphony.docweave.api.dto.IngestionResponse;
import com.symphony.docweave.service.DocumentIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentIngestionController {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionController.class);

    private final DocumentIngestionService ingestionService;

    public DocumentIngestionController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestionResponse> ingestDocument(@RequestParam("file") MultipartFile file) {
        log.info("Received ingestion request for file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        IngestionResponse response = ingestionService.ingestDocument(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID documentId) {
        DocumentResponse response = ingestionService.getDocument(documentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        List<DocumentResponse> documents = ingestionService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{documentId}/chunks")
    public ResponseEntity<List<ChunkResponse>> getChunks(@PathVariable UUID documentId) {
        List<ChunkResponse> chunks = ingestionService.getChunksByDocumentId(documentId);
        return ResponseEntity.ok(chunks);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID documentId) {
        ingestionService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
