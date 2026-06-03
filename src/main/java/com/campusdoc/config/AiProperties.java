package com.campusdoc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String apiKey;
    private ChatConfig chat = new ChatConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    private double rejectScoreThreshold = 0.6;
    private double hotqaSimilarityThreshold = 0.95;
    private int hotqaTtlHours = 24;

    @Data
    public static class ChatConfig {
        private String baseUrl;
        private String model;
        private String systemPrompt;
    }

    @Data
    public static class EmbeddingConfig {
        private String baseUrl;
        private String model;
        private int dimension = 1024;
    }
}
