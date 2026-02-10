package com.symphony.docweave.api;

import com.symphony.docweave.api.dto.ChunkResponse;
import com.symphony.docweave.api.dto.DocumentResponse;
import com.symphony.docweave.api.dto.IngestionResponse;
import com.symphony.docweave.exception.DocumentProcessingException;
import com.symphony.docweave.service.DocumentIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentIngestionController.class)
class DocumentIngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentIngestionService ingestionService;

    // --- POST /api/v1/documents ---

    @Test
    void ingestDocument_shouldReturn201OnSuccess() throws Exception {
        UUID docId = UUID.randomUUID();
        IngestionResponse response = new IngestionResponse(docId, "test.pdf", 5, "COMPLETED", Instant.now());
        when(ingestionService.ingestDocument(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "pdf content".getBytes());

        mockMvc.perform(multipart("/api/v1/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").value(docId.toString()))
                .andExpect(jsonPath("$.filename").value("test.pdf"))
                .andExpect(jsonPath("$.totalChunks").value(5))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void ingestDocument_shouldReturn400ForEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/v1/documents").file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestDocument_shouldReturn409ForDuplicate() throws Exception {
        when(ingestionService.ingestDocument(any()))
                .thenThrow(new DocumentProcessingException("Document already ingested with ID: some-id"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "dup.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/documents").file(file))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already ingested")));
    }

    @Test
    void ingestDocument_shouldReturn422ForProcessingError() throws Exception {
        when(ingestionService.ingestDocument(any()))
                .thenThrow(new DocumentProcessingException("No text could be extracted from: bad.pdf"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/documents").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("No text could be extracted")));
    }

    // --- GET /api/v1/documents ---

    @Test
    void getAllDocuments_shouldReturn200WithList() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<DocumentResponse> docs = List.of(
                new DocumentResponse(id1, "pdf", "a.pdf", "checksum1", Instant.now()),
                new DocumentResponse(id2, "pdf", "b.pdf", "checksum2", Instant.now())
        );
        when(ingestionService.getAllDocuments()).thenReturn(docs);

        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].originalFilename").value("a.pdf"))
                .andExpect(jsonPath("$[1].originalFilename").value("b.pdf"));
    }

    @Test
    void getAllDocuments_shouldReturn200WithEmptyList() throws Exception {
        when(ingestionService.getAllDocuments()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /api/v1/documents/{id} ---

    @Test
    void getDocument_shouldReturn200() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentResponse response = new DocumentResponse(docId, "pdf", "test.pdf", "abc", Instant.now());
        when(ingestionService.getDocument(docId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/documents/{id}", docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.originalFilename").value("test.pdf"));
    }

    @Test
    void getDocument_shouldReturn404WhenNotFound() throws Exception {
        UUID docId = UUID.randomUUID();
        when(ingestionService.getDocument(docId))
                .thenThrow(new DocumentProcessingException("Document not found: " + docId));

        mockMvc.perform(get("/api/v1/documents/{id}", docId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    // --- GET /api/v1/documents/{id}/chunks ---

    @Test
    void getChunks_shouldReturn200WithChunks() throws Exception {
        UUID docId = UUID.randomUUID();
        List<ChunkResponse> chunks = List.of(
                new ChunkResponse(UUID.randomUUID(), docId, 0, "chunk zero"),
                new ChunkResponse(UUID.randomUUID(), docId, 1, "chunk one")
        );
        when(ingestionService.getChunksByDocumentId(docId)).thenReturn(chunks);

        mockMvc.perform(get("/api/v1/documents/{id}/chunks", docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].chunkIndex").value(0))
                .andExpect(jsonPath("$[0].content").value("chunk zero"))
                .andExpect(jsonPath("$[1].chunkIndex").value(1));
    }

    @Test
    void getChunks_shouldReturn404WhenDocumentNotFound() throws Exception {
        UUID docId = UUID.randomUUID();
        when(ingestionService.getChunksByDocumentId(docId))
                .thenThrow(new DocumentProcessingException("Document not found: " + docId));

        mockMvc.perform(get("/api/v1/documents/{id}/chunks", docId))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/v1/documents/{id} ---

    @Test
    void deleteDocument_shouldReturn204() throws Exception {
        UUID docId = UUID.randomUUID();
        doNothing().when(ingestionService).deleteDocument(docId);

        mockMvc.perform(delete("/api/v1/documents/{id}", docId))
                .andExpect(status().isNoContent());

        verify(ingestionService).deleteDocument(docId);
    }

    @Test
    void deleteDocument_shouldReturn404WhenNotFound() throws Exception {
        UUID docId = UUID.randomUUID();
        doThrow(new DocumentProcessingException("Document not found: " + docId))
                .when(ingestionService).deleteDocument(docId);

        mockMvc.perform(delete("/api/v1/documents/{id}", docId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }
}
