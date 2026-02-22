package com.symphony.docweave.util;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingClient.class);

    private static final String MODEL = "text-embedding-3-small";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final OpenAiService openAiService;

    /**
     * Calls the OpenAI Embeddings API for the given texts and returns one float[]
     * per input, in the same order. Retries up to MAX_RETRIES times with
     * exponential back-off on any failure.
     */
    public List<float[]> embed(List<String> texts) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                EmbeddingRequest request = EmbeddingRequest.builder()
                        .model(MODEL)
                        .input(texts)
                        .build();

                EmbeddingResult result = openAiService.createEmbeddings(request);

                return result.getData().stream()
                        .map(Embedding::getEmbedding)
                        .map(EmbeddingUtils::toFloatArray)
                        .toList();

            } catch (Exception e) {
                lastException = e;
                long delay = RETRY_DELAY_MS * (1L << (attempt - 1)); // 1 s, 2 s, 4 s
                log.warn("OpenAI embed call failed (attempt {}/{}): {}. Retrying in {} ms",
                        attempt, MAX_RETRIES, e.getMessage(), delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry back-off", ie);
                }
            }
        }

        throw new RuntimeException("OpenAI embed failed after " + MAX_RETRIES + " attempts", lastException);
    }
}
