package com.symphony.docweave.service;

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
import com.symphony.docweave.service.impl.DocumentIngestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceImplTest {

    @Mock
    private DocumentTextExtractor textExtractor;

    @Mock
    private TextChunker textChunker;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository chunkRepository;

    private DocumentIngestionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocumentIngestionServiceImpl(textExtractor, textChunker, documentRepository, chunkRepository);
    }

    // --- ingestDocument tests ---

    @Test
    void ingestDocument_shouldExtractChunkAndPersist() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "pdf content".getBytes());

        when(documentRepository.findByChecksum(anyString())).thenReturn(Optional.empty());
        when(textExtractor.extract(any(InputStream.class), eq("test.pdf")))
                .thenReturn("This is the extracted text from the document");
        when(textChunker.chunk(anyString(), anyString()))
                .thenReturn(List.of(
                        new DocumentChunk("c1", "doc1", 0, "This is the"),
                        new DocumentChunk("c2", "doc1", 1, "extracted text")
                ));
        when(documentRepository.save(any(DocumentEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(chunkRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        IngestionResponse response = service.ingestDocument(file);

        assertNotNull(response);
        assertEquals("test.pdf", response.getFilename());
        assertEquals(2, response.getTotalChunks());
        assertEquals("COMPLETED", response.getStatus());
        assertNotNull(response.getDocumentId());
        assertNotNull(response.getCreatedAt());

        verify(documentRepository).save(any(DocumentEntity.class));
        verify(chunkRepository).saveAll(anyList());
    }

    @Test
    void ingestDocument_shouldRejectDuplicateByChecksum() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "pdf content".getBytes());

        UUID existingId = UUID.randomUUID();
        DocumentEntity existing = new DocumentEntity(existingId, "application/pdf", "existing.pdf", "checksum");
        when(documentRepository.findByChecksum(anyString())).thenReturn(Optional.of(existing));

        DocumentProcessingException ex = assertThrows(DocumentProcessingException.class,
                () -> service.ingestDocument(file));

        assertTrue(ex.getMessage().contains("already ingested"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void ingestDocument_shouldThrowWhenNoTextExtracted() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", "empty".getBytes());

        when(documentRepository.findByChecksum(anyString())).thenReturn(Optional.empty());
        when(textExtractor.extract(any(InputStream.class), eq("empty.pdf")))
                .thenReturn("");

        DocumentProcessingException ex = assertThrows(DocumentProcessingException.class,
                () -> service.ingestDocument(file));

        assertTrue(ex.getMessage().contains("No text could be extracted"));
    }

    @Test
    void ingestDocument_shouldThrowWhenExtractionFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.pdf", "application/pdf", "bad".getBytes());

        when(documentRepository.findByChecksum(anyString())).thenReturn(Optional.empty());
        when(textExtractor.extract(any(InputStream.class), eq("bad.pdf")))
                .thenThrow(new DocumentProcessingException("Extraction failed"));

        DocumentProcessingException ex = assertThrows(DocumentProcessingException.class,
                () -> service.ingestDocument(file));

        assertTrue(ex.getMessage().contains("Extraction failed"));
    }

    @Test
    void ingestDocument_shouldPersistCorrectDocumentEntity() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "content".getBytes());

        when(documentRepository.findByChecksum(anyString())).thenReturn(Optional.empty());
        when(textExtractor.extract(any(InputStream.class), anyString())).thenReturn("Some text");
        when(textChunker.chunk(anyString(), anyString())).thenReturn(List.of(
                new DocumentChunk("c1", "doc1", 0, "Some text")));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chunkRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        service.ingestDocument(file);

        ArgumentCaptor<DocumentEntity> captor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(captor.capture());

        DocumentEntity saved = captor.getValue();
        assertEquals("report.pdf", saved.getOriginalFilename());
        assertEquals("application/pdf", saved.getSource());
        assertNotNull(saved.getChecksum());
        assertNotNull(saved.getId());
    }

    // --- getDocument tests ---

    @Test
    void getDocument_shouldReturnDocumentResponse() {
        UUID docId = UUID.randomUUID();
        DocumentEntity entity = new DocumentEntity(docId, "application/pdf", "test.pdf", "abc123");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(entity));

        DocumentResponse response = service.getDocument(docId);

        assertEquals(docId, response.getId());
        assertEquals("test.pdf", response.getOriginalFilename());
        assertEquals("abc123", response.getChecksum());
    }

    @Test
    void getDocument_shouldThrowWhenNotFound() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        DocumentProcessingException ex = assertThrows(DocumentProcessingException.class,
                () -> service.getDocument(docId));

        assertTrue(ex.getMessage().contains("not found"));
    }

    // --- getAllDocuments tests ---

    @Test
    void getAllDocuments_shouldReturnAllDocuments() {
        DocumentEntity e1 = new DocumentEntity(UUID.randomUUID(), "pdf", "a.pdf", "c1");
        DocumentEntity e2 = new DocumentEntity(UUID.randomUUID(), "pdf", "b.pdf", "c2");
        when(documentRepository.findAll()).thenReturn(List.of(e1, e2));

        List<DocumentResponse> results = service.getAllDocuments();

        assertEquals(2, results.size());
    }

    @Test
    void getAllDocuments_shouldReturnEmptyListWhenNoDocuments() {
        when(documentRepository.findAll()).thenReturn(Collections.emptyList());

        List<DocumentResponse> results = service.getAllDocuments();

        assertTrue(results.isEmpty());
    }

    // --- getChunksByDocumentId tests ---

    @Test
    void getChunksByDocumentId_shouldReturnChunks() {
        UUID docId = UUID.randomUUID();
        DocumentEntity docEntity = new DocumentEntity(docId, "pdf", "test.pdf", "checksum");
        DocumentChunkEntity chunk1 = new DocumentChunkEntity(UUID.randomUUID(), docEntity, 0, "chunk one");
        DocumentChunkEntity chunk2 = new DocumentChunkEntity(UUID.randomUUID(), docEntity, 1, "chunk two");

        when(documentRepository.existsById(docId)).thenReturn(true);
        when(chunkRepository.findByDocumentIdOrderByChunkIndex(docId))
                .thenReturn(List.of(chunk1, chunk2));

        List<ChunkResponse> results = service.getChunksByDocumentId(docId);

        assertEquals(2, results.size());
        assertEquals(0, results.get(0).getChunkIndex());
        assertEquals("chunk one", results.get(0).getContent());
        assertEquals(1, results.get(1).getChunkIndex());
        assertEquals("chunk two", results.get(1).getContent());
    }

    @Test
    void getChunksByDocumentId_shouldThrowWhenDocumentNotFound() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.existsById(docId)).thenReturn(false);

        DocumentProcessingException ex = assertThrows(DocumentProcessingException.class,
                () -> service.getChunksByDocumentId(docId));

        assertTrue(ex.getMessage().contains("not found"));
    }

    // --- deleteDocument tests ---

    @Test
    void deleteDocument_shouldDeleteDocumentAndChunks() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.existsById(docId)).thenReturn(true);

        service.deleteDocument(docId);

        verify(chunkRepository).deleteByDocumentId(docId);
        verify(documentRepository).deleteById(docId);
    }

    @Test
    void deleteDocument_shouldThrowWhenNotFound() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.existsById(docId)).thenReturn(false);

        DocumentProcessingException ex = assertThrows(DocumentProcessingException.class,
                () -> service.deleteDocument(docId));

        assertTrue(ex.getMessage().contains("not found"));
        verify(chunkRepository, never()).deleteByDocumentId(any());
        verify(documentRepository, never()).deleteById(any());
    }
}
