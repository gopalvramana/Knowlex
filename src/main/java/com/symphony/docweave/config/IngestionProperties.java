package com.symphony.docweave.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ingestion")
@Getter
@Setter
public class IngestionProperties {

    private int chunkSize = 200;
    private int chunkOverlap = 40;
}
