package com.symphony.docweave.service;

import com.symphony.docweave.api.dto.RagRequest;
import com.symphony.docweave.api.dto.RagResponse;
import com.symphony.docweave.api.dto.SearchResult;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final SemanticSearchService searchService;
    private final OpenAiService openAiService;

    @Value("${rag.model:gpt-4o-mini}")
    private String model;

    @Value("${rag.max-tokens:1024}")
    private int maxTokens;

    @Value("${rag.temperature:0.2}")
    private double temperature;

    @Value("${rag.default-top-k:5}")
    private int defaultTopK;

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions strictly based on the provided context.
            Each context chunk is delimited by "---".
            If the answer cannot be found in the context, say "I don't have enough information to answer that."
            Be concise and accurate. Do not make up information.
            """;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Full RAG pipeline:
     * 1. Validate the incoming request
     * 2. Embed the query and retrieve the top-K most similar chunks
     * 3. Build a prompt from those chunks and send it to the LLM
     * 4. Return the LLM answer together with the source chunks for citation
     *
     * @param request contains the user query and optional topK override
     * @return LLM answer plus the chunks used as context
     */
    public RagResponse answer(RagRequest request) {
        validate(request);

        String query = request.getQuery().trim();
        int k = request.getTopK() != null && request.getTopK() > 0 ? request.getTopK() : defaultTopK;

        log.info("RAG pipeline | k={} | query=\"{}\"", k, query);

        // Step 1: Retrieve relevant chunks via vector similarity
        List<SearchResult> chunks = searchService.search(query, k);

        if (chunks.isEmpty()) {
            log.warn("No relevant chunks found for query: {}", query);
            return new RagResponse(query,
                    "I don't have enough information to answer that.",
                    List.of());
        }

        // Step 2: Build the context block from retrieved chunks
        String context = buildContext(chunks);

        // Step 3: Call the LLM with system + context + user question
        String answer = callLlm(context, query);

        log.info("RAG pipeline complete | chunks_used={}", chunks.size());
        return new RagResponse(query, answer, chunks);
    }

    // -------------------------------------------------------------------------
    // Prompt building
    // -------------------------------------------------------------------------

    /**
     * Joins chunk contents into a numbered, delimited context block, e.g.:
     *
     * [1]
     * <chunk text>
     * ---
     * [2]
     * <chunk text>
     * ---
     */
    private String buildContext(List<SearchResult> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            sb.append("[").append(i + 1).append("]\n");
            sb.append(chunks.get(i).getContent().strip());
            sb.append("\n---\n");
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // LLM call
    // -------------------------------------------------------------------------

    /**
     * Sends a three-message conversation to the chat completion API:
     *   system  → persona + answering rules
     *   user    → context chunks
     *   user    → the actual question
     *
     * Returns the content of the first completion choice.
     */
    private String callLlm(String context, String query) {
        List<ChatMessage> messages = buildMessages(context, query);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

        log.debug("Calling LLM model={} maxTokens={} temperature={}", model, maxTokens, temperature);

        return openAiService.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
    }

    private List<ChatMessage> buildMessages(String context, String query) {
        List<ChatMessage> messages = new ArrayList<>();

        // 1. Persona + rules
        messages.add(new ChatMessage(
                ChatMessageRole.SYSTEM.value(),
                SYSTEM_PROMPT));

        // 2. Retrieved context (sent as a user turn so it stays within the
        //    context window without inflating the system prompt size)
        messages.add(new ChatMessage(
                ChatMessageRole.USER.value(),
                "Here is the relevant context:\n\n" + context));

        // 3. The actual user question
        messages.add(new ChatMessage(
                ChatMessageRole.USER.value(),
                "Question: " + query));

        return messages;
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private static void validate(RagRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        if (!StringUtils.hasText(request.getQuery())) {
            throw new IllegalArgumentException("Query must not be blank");
        }
    }
}
