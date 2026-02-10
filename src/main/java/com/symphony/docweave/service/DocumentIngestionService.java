package com.symphony.docweave.service;

import com.symphony.docweave.api.dto.ChunkResponse;
import com.symphony.docweave.api.dto.DocumentResponse;
import com.symphony.docweave.api.dto.IngestionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentIngestionService {

    IngestionResponse ingestDocument(MultipartFile file);

    DocumentResponse getDocument(UUID documentId);

    List<DocumentResponse> getAllDocuments();

    List<ChunkResponse> getChunksByDocumentId(UUID documentId);

    void deleteDocument(UUID documentId);
}
