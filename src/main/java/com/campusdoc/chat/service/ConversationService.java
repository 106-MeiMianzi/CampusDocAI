package com.campusdoc.chat.service;

import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.chat.dto.ChatMessageResponse;
import com.campusdoc.chat.dto.CitationResponse;
import com.campusdoc.chat.dto.ConversationResponse;
import com.campusdoc.chat.dto.CreateConversationRequest;
import com.campusdoc.chat.entity.ChatMessageEntity;
import com.campusdoc.chat.entity.ConversationEntity;
import com.campusdoc.chat.mapper.ChatMessageMapper;
import com.campusdoc.chat.mapper.ConversationMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ConversationService {

    private static final String LIST_CACHE_PREFIX = "campus:conversation:list:";
    private static final long LIST_CACHE_SECONDS = 30;

    private final ConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMemoryService chatMemoryService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConversationService(ConversationMapper conversationMapper,
                               ChatMessageMapper chatMessageMapper,
                               ChatMemoryService chatMemoryService,
                               StringRedisTemplate redisTemplate) {
        this.conversationMapper = conversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatMemoryService = chatMemoryService;
        this.redisTemplate = redisTemplate;
    }

    public ConversationResponse create(Long userId, CreateConversationRequest request) {
        ConversationEntity entity = new ConversationEntity();
        entity.setUserId(userId);
        entity.setTitle(request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle() : "新会话");
        conversationMapper.insert(entity);
        evictListCache(userId);
        return new ConversationResponse(entity.getId(), entity.getTitle(), entity.getCreatedAt());
    }

    public List<ConversationResponse> list(Long userId) {
        String cacheKey = LIST_CACHE_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isBlank()) {
            try {
                return objectMapper.readValue(cached, new TypeReference<>() {
                });
            } catch (Exception ignored) {
                redisTemplate.delete(cacheKey);
            }
        }
        List<ConversationResponse> result = conversationMapper.listByUserId(userId).stream()
                .map(c -> new ConversationResponse(c.getId(), c.getTitle(), c.getCreatedAt()))
                .toList();
        try {
            redisTemplate.opsForValue().set(
                    cacheKey, objectMapper.writeValueAsString(result), LIST_CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // best effort cache
        }
        return result;
    }

    public List<ChatMessageResponse> messages(Long userId, Long conversationId) {
        requireOwned(userId, conversationId);
        return chatMessageMapper.listByConversationId(conversationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long conversationId) {
        requireOwned(userId, conversationId);
        chatMessageMapper.deleteByConversationId(conversationId);
        conversationMapper.deleteByIdAndUserId(conversationId, userId);
        chatMemoryService.clear(conversationId);
        evictListCache(userId);
    }

    public void evictListCache(Long userId) {
        redisTemplate.delete(LIST_CACHE_PREFIX + userId);
    }

    public void evictListCacheByConversationId(Long conversationId) {
        ConversationEntity entity = conversationMapper.findById(conversationId);
        if (entity != null) {
            evictListCache(entity.getUserId());
        }
    }

    public ConversationEntity requireOwned(Long userId, Long conversationId) {
        ConversationEntity entity = conversationMapper.findByIdAndUserId(conversationId, userId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
        }
        return entity;
    }

    private ChatMessageResponse toResponse(ChatMessageEntity entity) {
        List<CitationResponse> citations = Collections.emptyList();
        if (entity.getCitationsJson() != null && !entity.getCitationsJson().isBlank()) {
            try {
                citations = objectMapper.readValue(entity.getCitationsJson(),
                        new TypeReference<List<CitationResponse>>() {
                        });
            } catch (Exception ignored) {
                citations = Collections.emptyList();
            }
        }
        return new ChatMessageResponse(
                entity.getId(), entity.getRole(), entity.getContent(), citations, entity.getCreatedAt());
    }
}
