package com.campusdoc.chat.service;

import com.campusdoc.ai.client.ChatClient;
import com.campusdoc.ai.client.EmbeddingClient;
import com.campusdoc.ai.service.VectorStoreService;
import com.campusdoc.chat.dto.*;
import com.campusdoc.chat.entity.ChatMessageEntity;
import com.campusdoc.chat.entity.ConversationEntity;
import com.campusdoc.chat.mapper.ChatMessageMapper;
import com.campusdoc.chat.mapper.ConversationMapper;
import com.campusdoc.config.AiProperties;
import com.campusdoc.document.entity.DocumentEntity;
import com.campusdoc.document.service.DocumentScopedSearchService;
import com.campusdoc.document.service.DocumentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;
    private final AiProperties aiProperties;
    private final ConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ConversationService conversationService;
    private final ChatMemoryService chatMemoryService;
    private final HotQaCacheService hotQaCacheService;
    private final DocumentService documentService;
    private final DocumentScopedSearchService documentScopedSearchService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int defaultTopK;

    public ChatService(ChatClient chatClient,
                       EmbeddingClient embeddingClient,
                       VectorStoreService vectorStoreService,
                       AiProperties aiProperties,
                       ConversationMapper conversationMapper,
                       ChatMessageMapper chatMessageMapper,
                       ConversationService conversationService,
                       ChatMemoryService chatMemoryService,
                       HotQaCacheService hotQaCacheService,
                       DocumentService documentService,
                       DocumentScopedSearchService documentScopedSearchService,
                       @Value("${search.default-top-k:5}") int defaultTopK) {
        this.chatClient = chatClient;
        this.embeddingClient = embeddingClient;
        this.vectorStoreService = vectorStoreService;
        this.aiProperties = aiProperties;
        this.conversationMapper = conversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.conversationService = conversationService;
        this.chatMemoryService = chatMemoryService;
        this.hotQaCacheService = hotQaCacheService;
        this.documentService = documentService;
        this.documentScopedSearchService = documentScopedSearchService;
        this.defaultTopK = defaultTopK;
    }

    @Transactional
    public AskResponse ask(Long userId, AskRequest request) {
        AskResponse cached = hotQaCacheService.getCached(request.getQuestion());
        if (cached != null) {
            return cached;
        }

        Long conversationId = request.getConversationId();
        if (conversationId == null) {
            ConversationEntity created = new ConversationEntity();
            created.setUserId(userId);
            created.setTitle(trimTitle(request.getQuestion()));
            conversationMapper.insert(created);
            conversationId = created.getId();
        } else {
            conversationService.requireOwned(userId, conversationId);
        }

        Long documentId = request.getDocumentId();
        DocumentEntity scopedDoc = null;
        if (documentId != null) {
            scopedDoc = documentService.requireAccessible(userId, documentId);
        }

        float[] queryVector = embeddingClient.embed(request.getQuestion());
        List<VectorStoreService.ScoredChunk> hits;
        if (documentId != null) {
            hits = documentScopedSearchService.search(
                    documentId, scopedDoc.getFileName(), queryVector, defaultTopK);
        } else {
            hits = vectorStoreService.search(userId, null, queryVector, defaultTopK);
        }
        double maxScore = hits.stream().mapToDouble(VectorStoreService.ScoredChunk::score).max().orElse(0);

        List<CitationResponse> citations = hits.stream()
                .map(h -> new CitationResponse(h.fileName(), h.content()))
                .collect(Collectors.toList());

        boolean scopedToDocument = documentId != null;
        if (hits.isEmpty() || (!scopedToDocument && maxScore < aiProperties.getRejectScoreThreshold())) {
            AskResponse reject = new AskResponse(
                    conversationId,
                    scopedToDocument ? "当前提供的文档中未找到与您问题相关的内容。" : "当前知识库中未找到相关依据。",
                    List.of(),
                    "请联系教务处或上传相关制度文档。");
            persistMessages(conversationId, request.getQuestion(), reject);
            return reject;
        }

        String context = hits.stream()
                .map(h -> "【" + h.fileName() + "】\n" + h.content())
                .collect(Collectors.joining("\n\n"));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", aiProperties.getChat().getSystemPrompt()));
        for (Map<String, String> history : chatMemoryService.load(conversationId)) {
            messages.add(history);
        }
        messages.add(Map.of("role", "user", "content",
                "参考文档片段：\n" + context + "\n\n用户问题：" + request.getQuestion()));

        String answer = chatClient.chat(messages);
        AskResponse response = new AskResponse(conversationId, answer, citations, null);
        persistMessages(conversationId, request.getQuestion(), response);
        hotQaCacheService.put(request.getQuestion(), response);
        return response;
    }

    private void persistMessages(Long conversationId, String question, AskResponse response) {
        saveMessage(conversationId, "user", question, null);
        try {
            String citationsJson = response.getCitations() == null ? null
                    : objectMapper.writeValueAsString(response.getCitations());
            saveMessage(conversationId, "assistant", response.getAnswer(), citationsJson);
            List<Map<String, String>> memory = new ArrayList<>(chatMemoryService.load(conversationId));
            memory.add(Map.of("role", "user", "content", question));
            memory.add(Map.of("role", "assistant", "content", response.getAnswer()));
            if (memory.size() > 20) {
                memory = memory.subList(memory.size() - 20, memory.size());
            }
            chatMemoryService.save(conversationId, memory);
        } catch (JsonProcessingException e) {
            log.warn("Failed to persist chat messages for conversation {}: {}", conversationId, e.getMessage());
        }
    }

    private void saveMessage(Long conversationId, String role, String content, String citationsJson) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setConversationId(conversationId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setCitationsJson(citationsJson);
        chatMessageMapper.insert(entity);
    }

    private String trimTitle(String question) {
        if (question.length() <= 30) {
            return question;
        }
        return question.substring(0, 30) + "...";
    }
}
