package com.campusdoc.document.service;

import com.campusdoc.config.DocumentProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class DocumentPreviewTokenService {

    private static final String KEY_PREFIX = "campus:preview:doc:";

    private final StringRedisTemplate redisTemplate;
    private final DocumentProperties documentProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentPreviewTokenService(StringRedisTemplate redisTemplate,
                                       DocumentProperties documentProperties) {
        this.redisTemplate = redisTemplate;
        this.documentProperties = documentProperties;
    }

    public String create(Long userId, Long documentId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        PreviewPayload payload = new PreviewPayload(userId, documentId);
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + token,
                    json,
                    documentProperties.getPreviewTokenTtlMinutes(),
                    TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to create preview token", e);
        }
        return token;
    }

    public int ttlSeconds() {
        return documentProperties.getPreviewTokenTtlMinutes() * 60;
    }

    public Optional<PreviewPayload> validate(String token, Long documentId) {
        if (token == null || token.isBlank() || documentId == null) {
            return Optional.empty();
        }
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + token.trim());
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            PreviewPayload payload = objectMapper.readValue(json, PreviewPayload.class);
            if (payload.documentId() == null || !payload.documentId().equals(documentId)) {
                return Optional.empty();
            }
            return Optional.of(payload);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public record PreviewPayload(Long userId, Long documentId) {
    }
}
