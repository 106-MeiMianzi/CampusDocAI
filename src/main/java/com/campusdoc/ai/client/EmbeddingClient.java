package com.campusdoc.ai.client;

import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Integer cachedDimension;

    public EmbeddingClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    public float[] embed(String text) {
        List<float[]> vectors = embedBatch(List.of(text));
        return vectors.get(0);
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        String url = aiProperties.getEmbedding().getBaseUrl() + "/services/embeddings/text-embedding/text-embedding";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());
        Map<String, Object> body = Map.of(
                "model", aiProperties.getEmbedding().getModel(),
                "input", Map.of("texts", texts)
        );
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddings = root.path("output").path("embeddings");
            if (!embeddings.isArray() || embeddings.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_CALL_FAILED, "AI 调用失败: embedding 响应为空");
            }
            List<float[]> result = new ArrayList<>();
            for (JsonNode item : embeddings) {
                JsonNode vectorNode = item.path("embedding");
                float[] vector = new float[vectorNode.size()];
                for (int i = 0; i < vectorNode.size(); i++) {
                    vector[i] = (float) vectorNode.get(i).asDouble();
                }
                if (cachedDimension == null) {
                    cachedDimension = vector.length;
                }
                result.add(vector);
            }
            return result;
        } catch (RestClientException | IOException e) {
            log.error("Embedding call failed", e);
            throw new BusinessException(ErrorCode.AI_CALL_FAILED, "AI 调用失败: " + e.getMessage());
        }
    }

    public int dimension() {
        if (cachedDimension != null) {
            return cachedDimension;
        }
        embed("dimension probe");
        return cachedDimension != null ? cachedDimension : 1024;
    }
}
