package com.symphony.docweave.config;

import com.theokanning.openai.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAIConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenAIConfig.class);

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.timeout-seconds:30}")
    private int timeoutSeconds;

    @Bean
    public OpenAiService openAiService() {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("${OPENAI_API_KEY}")) {
            throw new IllegalStateException(
                "OpenAI API key is not set. " +
                "Export it before starting the app: export OPENAI_API_KEY=sk-...");
        }
        log.info("OpenAI client initialised (timeout={}s)", timeoutSeconds);
        return new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
    }
}
