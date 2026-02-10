package com.symphony.docweave.extractor;

import com.symphony.docweave.domain.Document;
import com.symphony.docweave.domain.DocumentType;
import com.symphony.docweave.exception.DocumentProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfBoxDocumentExtractorTest {

    private PdfBoxDocumentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PdfBoxDocumentExtractor();
    }

    @Test
    void extract_withInputStream_shouldExtractText() throws Exception {
        byte[] pdfBytes = createTestPdf("Hello World from test PDF");
        InputStream inputStream = new ByteArrayInputStream(pdfBytes);

        String result = extractor.extract(inputStream, "test.pdf");

        assertNotNull(result);
        assertTrue(result.contains("Hello World from test PDF"));
    }

    @Test
    void extract_withInputStream_shouldThrowOnInvalidPdf() {
        InputStream inputStream = new ByteArrayInputStream("not a pdf".getBytes());

        assertThrows(DocumentProcessingException.class, () ->
                extractor.extract(inputStream, "invalid.pdf"));
    }

    @Test
    void extract_withDocument_shouldRejectNonPdfType() {
        Document docxDocument = new Document("test.docx", DocumentType.DOCX);

        assertThrows(IllegalArgumentException.class, () ->
                extractor.extract(docxDocument));
    }

    @Test
    void extract_withDocument_shouldRejectHtmlType() {
        Document htmlDocument = new Document("test.html", DocumentType.HTML);

        assertThrows(IllegalArgumentException.class, () ->
                extractor.extract(htmlDocument));
    }

    @Test
    void extract_withDocument_shouldAcceptPdfType(@TempDir Path tempDir) throws Exception {
        // Create a temporary PDF file
        File pdfFile = tempDir.resolve("test.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 700);
                content.showText("Test content in PDF file");
                content.endText();
            }
            doc.save(pdfFile);
        }

        Document document = new Document(pdfFile.getAbsolutePath(), DocumentType.PDF);
        String result = extractor.extract(document);

        assertNotNull(result);
        assertTrue(result.contains("Test content in PDF file"));
    }

    @Test
    void extract_withInputStream_shouldHandleMultiplePages() throws Exception {
        byte[] pdfBytes = createMultiPagePdf("Page one text", "Page two text");
        InputStream inputStream = new ByteArrayInputStream(pdfBytes);

        String result = extractor.extract(inputStream, "multipage.pdf");

        assertNotNull(result);
        assertTrue(result.contains("Page one text"));
        assertTrue(result.contains("Page two text"));
    }

    @Test
    void extract_withInputStream_shouldHandleEmptyPdf() throws Exception {
        // PDF with a page but no text
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(baos);
        }
        InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());

        String result = extractor.extract(inputStream, "empty.pdf");

        assertNotNull(result);
        assertTrue(result.isBlank());
    }

    private byte[] createTestPdf(String text) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            doc.save(baos);
        }
        return baos.toByteArray();
    }

    private byte[] createMultiPagePdf(String... pageTexts) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(text);
                    content.endText();
                }
            }
            doc.save(baos);
        }
        return baos.toByteArray();
    }
}
