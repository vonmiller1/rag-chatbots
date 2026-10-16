package com.example.rag.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        @Positive int chunkSize,
        @Min(0) int chunkOverlap,
        @Positive int defaultTopK,
        @Positive int maxContextChars,
        double minRelevanceScore
) {
}
