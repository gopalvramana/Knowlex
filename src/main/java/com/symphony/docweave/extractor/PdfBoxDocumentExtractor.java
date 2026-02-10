package com.symphony.docweave.extractor;

import com.symphony.docweave.domain.Document;
import com.symphony.docweave.domain.DocumentType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class PdfBoxDocumentExtractor implements DocumentTextExtractor {
    @Override
    public String extract(Document document) {

        // Validate document type - only PDF is supported
        if (document.getDocumentType() != DocumentType.PDF){
            throw new IllegalArgumentException("Unsupported document type: " + document.getDocumentType());
        }

         // Resolve the file path from the document's source name, which is expected to be a local file path
         // In a real implementation, this could involve downloading the file from a URL or accessing a storage service
         File pdfFile = resolveFile(document);

        // Use PDFBox to extract text from the PDF file
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(pdfDocument);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from PDF document: " + document.getSourceName(), e);
        }

    }

    private File resolveFile(Document document) {
        // For simplicity, we assume the source name is a local file path - Phase 1 implementation
        // In a real implementation, this method would handle different source types (e.g., URLs, cloud storage)
        return new File(document.getSourceName());
    }
}
