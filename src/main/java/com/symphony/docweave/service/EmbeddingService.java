package com.symphony.docweave.service;

import com.symphony.docweave.domain.DocumentChunkEntity;
import com.symphony.docweave.repository.DocumentChunkRepository;
import com.symphony.docweave.util.EmbeddingUtils;
import com.symphony.docweave.util.OpenAiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final OpenAiEmbeddingClient embeddingClient;
    private final DocumentChunkRepository chunkRepository;

    @Value("${embedding.batch-size:20}")
    private int batchSize;

    @Value("${embedding.parallelism:4}")
    private int parallelism;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public int generateEmbeddingsForAllChunks() {
        List<DocumentChunkEntity> pending = chunkRepository.findAll().stream()
                .filter(c -> c.getEmbedding() == null)
                .collect(Collectors.toList());

        log.info("Found {} chunk(s) without embeddings", pending.size());
        return processBatchesInParallel(pending);
    }

    public int generateEmbeddingsForDocument(UUID documentId) {
        List<DocumentChunkEntity> pending = chunkRepository
                .findByDocumentIdOrderByChunkIndex(documentId)
                .stream()
                .filter(c -> c.getEmbedding() == null)
                .collect(Collectors.toList());

        log.info("Document {}: {} chunk(s) need embeddings", documentId, pending.size());
        return processBatchesInParallel(pending);
    }

    public float[] generateEmbedding(String text) {
        return embeddingClient.embed(List.of(text)).get(0);
    }

    // -------------------------------------------------------------------------
    // Batch / parallel processing
    // -------------------------------------------------------------------------

    private int processBatchesInParallel(List<DocumentChunkEntity> chunks) {
        if (chunks.isEmpty()) return 0;

        List<List<DocumentChunkEntity>> batches = EmbeddingUtils.partition(chunks, batchSize);
        log.info("Processing {} batch(es) of up to {} chunks, parallelism={}",
                batches.size(), batchSize, parallelism);

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(parallelism, batches.size()));
        List<Future<Integer>> futures = new ArrayList<>();

        for (List<DocumentChunkEntity> batch : batches) {
            futures.add(executor.submit(() -> processBatch(batch)));
        }

        executor.shutdown();

        int total = 0;
        for (Future<Integer> f : futures) {
            try {
                total += f.get();
            } catch (Exception e) {
                log.error("A batch failed: {}", e.getMessage());
            }
        }

        log.info("Embedding complete. Total newly embedded: {}", total);
        return total;
    }

    private int processBatch(List<DocumentChunkEntity> batch) {
        List<String> texts = batch.stream().map(DocumentChunkEntity::getContent).toList();
        List<float[]> embeddings = embeddingClient.embed(texts);

        int saved = 0;
        for (int i = 0; i < batch.size(); i++) {
            try {
                saveEmbedding(batch.get(i).getId(), embeddings.get(i));
                saved++;
            } catch (Exception e) {
                log.error("Failed to save embedding for chunk {}: {}", batch.get(i).getId(), e.getMessage());
                System.out.println("Failed to save embedding for chunk " + batch.get(i).getId() + ": " + e.getMessage());
            }
        }
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveEmbedding(UUID chunkId, float[] embedding) {
        chunkRepository.findById(chunkId).ifPresent(chunk -> {
            chunk.setEmbedding(embedding);
            chunkRepository.save(chunk);
        });
    }
}
