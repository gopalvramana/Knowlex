package com.symphony.docweave.extractor;

import com.symphony.docweave.domain.Document;

public interface DocumentTextExtractor {

    String extract(Document document);

}
