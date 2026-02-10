package com.symphony.docweave.extractor;

import com.symphony.docweave.domain.Document;

import java.io.InputStream;

public interface DocumentTextExtractor {

    String extract(Document document);

    String extract(InputStream inputStream, String filename);
}
