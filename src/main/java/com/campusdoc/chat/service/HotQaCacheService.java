package com.campusdoc.chat.service;

import com.campusdoc.ai.service.VectorStoreService;
import com.campusdoc.chat.dto.AskResponse;
import com.campusdoc.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class HotQaCacheService {

    private static final String KEY_PREFIX = "campus:cache:hotqa:";
    private static final String INDEX_KEY = "campus:cache:hotqa:index";
    private static final int MAX_SIMILAR_CHECKS = 50;

    private final StringRedisTemplate redisTemplate;
    private final AiProperties aiProperties;
    private final VectorStoreService vectorStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HotQaCacheService(StringRedisTemplate redisTemplate,
                             AiProperties aiProperties,
                             VectorStoreService vectorStoreService) {
        this.redisTemplate = redisTemplate;
        this.aiProperties = aiProperties;
        this.vectorStoreService = vectorStoreService;
    }

    /** 精确匹配缓存，不触发 embedding，用于快速路径。 */
    public AskResponse getExact(String question) {
        String exactKey = KEY_PREFIX + sha256(question.trim());
        String exact = redisTemplate.opsForValue().get(exactKey);
        if (exact == null) {
            return null;
        }
        return deserialize(exact);
    }

    /**
     * 语义相似缓存：复用 ask 流程中已算好的向量，避免重复 embedding。
     * 使用索引 Set 遍历，不再调用 Redis KEYS。
     */
    public AskResponse findSimilar(float[] queryVector) {
        Set<String> keys = redisTemplate.opsForSet().members(INDEX_KEY);
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        int checked = 0;
        for (String key : keys) {
            if (checked >= MAX_SIMILAR_CHECKS) {
                break;
            }
            checked++;
            String payload = redisTemplate.opsForValue().get(key);
            if (payload == null) {
                redisTemplate.opsForSet().remove(INDEX_KEY, key);
                continue;
            }
            try {
                CachedEntry entry = objectMapper.readValue(payload, CachedEntry.class);
                if (entry.embedding() == null) {
                    continue;
                }
                double sim = vectorStoreService.cosineSimilarity(queryVector, entry.embedding());
                if (sim >= aiProperties.getHotqaSimilarityThreshold()) {
                    return entry.response();
                }
            } catch (Exception ignored) {
                // skip invalid cache entry
            }
        }
        return null;
    }

    public void put(String question, AskResponse response, float[] embedding) {
        try {
            CachedEntry entry = new CachedEntry(response, embedding);
            String json = objectMapper.writeValueAsString(entry);
            String key = KEY_PREFIX + sha256(question.trim());
            redisTemplate.opsForValue().set(key, json, aiProperties.getHotqaTtlHours(), TimeUnit.HOURS);
            redisTemplate.opsForSet().add(INDEX_KEY, key);
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
