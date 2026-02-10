package com.symphony.docweave.service.impl;

import com.symphony.docweave.api.dto.ChunkResponse;
import com.symphony.docweave.api.dto.DocumentResponse;
import com.symphony.docweave.api.dto.IngestionResponse;
import com.symphony.docweave.chunker.TextChunker;
import com.symphony.docweave.domain.DocumentChunk;
import com.symphony.docweave.domain.DocumentChunkEntity;
import com.symphony.docweave.domain.DocumentEntity;
import com.symphony.docweave.exception.DocumentProcessingException;
import com.symphony.docweave.extractor.DocumentTextExtractor;
import com.symphony.docweave.repository.DocumentChunkRepository;
import com.symphony.docweave.repository.DocumentRepository;
import com.symphony.docweave.service.DocumentIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionServiceImpl.class);

    private final DocumentTextExtractor textExtractor;
    private final TextChunker textChunker;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;

    public DocumentIngestionServiceImpl(DocumentTextExtractor textExtractor,
                                        TextChunker textChunker,
                                        DocumentRepository documentRepository,
                                        DocumentChunkRepository chunkRepository) {
        this.textExtractor = textExtractor;
        this.textChunker = textChunker;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    @Override
    @Transactional
    public IngestionResponse ingestDocument(MultipartFile file) {
        String filename = file.getOriginalFilename();
        log.info("Starting ingestion for file: {}", filename);

        // Compute checksum for deduplication
        String checksum = computeChecksum(file);
        documentRepository.findByChecksum(checksum).ifPresent(existing -> {
            throw new DocumentProcessingException(
                    "Document already ingested with ID: " + existing.getId());
        });

        // Extract text from the uploaded file
        String extractedText;
        try (InputStream inputStream = file.getInputStream()) {
            extractedText = textExtractor.extract(inputStream, filename);
        } catch (DocumentProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentProcessingException("Failed to read uploaded file: " + filename, e);
        }

        if (extractedText == null || extractedText.isBlank()) {
            throw new DocumentProcessingException("No text could be extracted from: " + filename);
        }

        // Persist the document record
        UUID documentId = UUID.randomUUID();
        DocumentEntity documentEntity = new DocumentEntity(
                documentId,
                file.getContentType(),
                filename,
                checksum
        );
        documentRepository.save(documentEntity);
        log.info("Saved document record: {} ({})", documentId, filename);

        // Chunk the extracted text
        List<DocumentChunk> chunks = textChunker.chunk(extractedText, documentId.toString());
        log.info("Generated {} chunks for document: {}", chunks.size(), documentId);

        // Persist chunks
        List<DocumentChunkEntity> chunkEntities = chunks.stream()
                .map(chunk -> new DocumentChunkEntity(
                        UUID.randomUUID(),
                        documentEntity,
                        chunk.getChunkIndex(),
                        chunk.getContent()
                ))
                .collect(Collectors.toList());
        chunkRepository.saveAll(chunkEntities);
        log.info("Saved {} chunks for document: {}", chunkEntities.size(), documentId);

        return new IngestionResponse(
                documentId,
                filename,
                chunkEntities.size(),
                "COMPLETED",
                documentEntity.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID documentId) {
        DocumentEntity entity = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));
        return toDocumentResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChunkResponse> getChunksByDocumentId(UUID documentId) {
        // Verify document exists
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentProcessingException("Document not found: " + documentId);
        }
        return chunkRepository.findByDocumentIdOrderByChunkIndex(documentId).stream()
                .map(chunk -> new ChunkResponse(
                        chunk.getId(),
                        documentId,
                        chunk.getChunkIndex(),
                        chunk.getContent()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentProcessingException("Document not found: " + documentId);
        }
        chunkRepository.deleteByDocumentId(documentId);
        documentRepository.deleteById(documentId);
        log.info("Deleted document and chunks: {}", documentId);
    }

    private DocumentResponse toDocumentResponse(DocumentEntity entity) {
        return new DocumentResponse(
                entity.getId(),
                entity.getSource(),
                entity.getOriginalFilename(),
                entity.getChecksum(),
                entity.getCreatedAt()
        );
    }

    private String computeChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new DocumentProcessingException("Failed to compute checksum", e);
        }
    }
}
