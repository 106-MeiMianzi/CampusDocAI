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
import java.util.List;
import java.util.Map;

@Component
public class ChatClient {

    private static final Logger log = LoggerFactory.getLogger(ChatClient.class);

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    public String chat(List<Map<String, String>> messages) {
        String url = aiProperties.getChat().getBaseUrl() + "/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());
        Map<String, Object> body = Map.of(
                "model", aiProperties.getChat().getModel(),
                "messages", messages
        );
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new BusinessException(ErrorCode.AI_CALL_FAILED, "AI 调用失败: chat 响应为空");
            }
            return content.asText();
        } catch (RestClientException | IOException e) {
            log.error("Chat call failed", e);
            throw new BusinessException(ErrorCode.AI_CALL_FAILED, "AI 调用失败: " + e.getMessage());
        }
    }
}
