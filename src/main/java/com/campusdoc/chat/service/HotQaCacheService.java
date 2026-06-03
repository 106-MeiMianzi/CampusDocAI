package com.campusdoc.chat.service;

import com.campusdoc.ai.client.EmbeddingClient;
import com.campusdoc.ai.service.VectorStoreService;
import com.campusdoc.chat.dto.AskResponse;
import com.campusdoc.chat.dto.CitationResponse;
import com.campusdoc.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class HotQaCacheService {

    private static final String KEY_PREFIX = "campus:cache:hotqa:";

    private final StringRedisTemplate redisTemplate;
    private final AiProperties aiProperties;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HotQaCacheService(StringRedisTemplate redisTemplate,
                             AiProperties aiProperties,
                             EmbeddingClient embeddingClient,
                             VectorStoreService vectorStoreService) {
        this.redisTemplate = redisTemplate;
        this.aiProperties = aiProperties;
        this.embeddingClient = embeddingClient;
        this.vectorStoreService = vectorStoreService;
    }

    public AskResponse getCached(String question) {
        String exactKey = KEY_PREFIX + sha256(question.trim());
        String exact = redisTemplate.opsForValue().get(exactKey);
        if (exact != null) {
            return deserialize(exact);
        }
        float[] qVec = embeddingClient.embed(question);
        for (String key : redisTemplate.keys(KEY_PREFIX + "*")) {
            if (key == null) {
                continue;
            }
            String payload = redisTemplate.opsForValue().get(key);
            if (payload == null) {
                continue;
            }
            try {
                CachedEntry entry = objectMapper.readValue(payload, CachedEntry.class);
                if (entry.embedding() == null) {
                    continue;
                }
                double sim = vectorStoreService.cosineSimilarity(qVec, entry.embedding());
                if (sim >= aiProperties.getHotqaSimilarityThreshold()) {
                    return entry.response();
                }
            } catch (Exception ignored) {
                // skip invalid cache entry
            }
        }
        return null;
    }

    public void put(String question, AskResponse response) {
        try {
            float[] embedding = embeddingClient.embed(question);
            CachedEntry entry = new CachedEntry(response, embedding);
            String json = objectMapper.writeValueAsString(entry);
            String key = KEY_PREFIX + sha256(question.trim());
            redisTemplate.opsForValue().set(key, json, aiProperties.getHotqaTtlHours(), TimeUnit.HOURS);
        } catch (Exception ignored) {
            // best effort cache
        }
    }

    private AskResponse deserialize(String json) {
        try {
            CachedEntry entry = objectMapper.readValue(json, CachedEntry.class);
            return entry.response();
        } catch (Exception e) {
            return null;
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public record CachedEntry(AskResponse response, float[] embedding) {
    }
}
