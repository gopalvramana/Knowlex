package com.symphony.docweave.api;

import com.symphony.docweave.api.dto.ErrorResponse;
import com.symphony.docweave.exception.DocumentProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleDocumentProcessingException_shouldReturn404ForNotFound() {
        DocumentProcessingException ex = new DocumentProcessingException("Document not found: abc");

        ResponseEntity<ErrorResponse> response = handler.handleDocumentProcessingException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("not found"));
    }

    @Test
    void handleDocumentProcessingException_shouldReturn409ForDuplicate() {
        DocumentProcessingException ex = new DocumentProcessingException("Document already ingested with ID: 123");

        ResponseEntity<ErrorResponse> response = handler.handleDocumentProcessingException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("already ingested"));
    }

    @Test
    void handleDocumentProcessingException_shouldReturn422ForOtherErrors() {
        DocumentProcessingException ex = new DocumentProcessingException("No text could be extracted");

        ResponseEntity<ErrorResponse> response = handler.handleDocumentProcessingException(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals(422, response.getBody().getStatus());
    }

    @Test
    void handleMaxSizeException_shouldReturn413() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(50 * 1024 * 1024);

        ResponseEntity<ErrorResponse> response = handler.handleMaxSizeException(ex);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(413, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("maximum allowed limit"));
    }

    @Test
    void handleIllegalArgument_shouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Unsupported document type: DOCX");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Unsupported document type"));
    }

    @Test
    void handleGenericException_shouldReturn500() {
        Exception ex = new RuntimeException("Something unexpected");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }

    @Test
    void errorResponse_shouldIncludeTimestamp() {
        DocumentProcessingException ex = new DocumentProcessingException("test error");

        ResponseEntity<ErrorResponse> response = handler.handleDocumentProcessingException(ex);

        assertNotNull(response.getBody().getTimestamp());
    }
}
