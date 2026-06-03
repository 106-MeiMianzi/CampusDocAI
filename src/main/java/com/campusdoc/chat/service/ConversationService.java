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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMemoryService chatMemoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConversationService(ConversationMapper conversationMapper,
                               ChatMessageMapper chatMessageMapper,
                               ChatMemoryService chatMemoryService) {
        this.conversationMapper = conversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatMemoryService = chatMemoryService;
    }

    public ConversationResponse create(Long userId, CreateConversationRequest request) {
        ConversationEntity entity = new ConversationEntity();
        entity.setUserId(userId);
        entity.setTitle(request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle() : "新会话");
        conversationMapper.insert(entity);
        return new ConversationResponse(entity.getId(), entity.getTitle(), entity.getCreatedAt());
    }

    public List<ConversationResponse> list(Long userId) {
        return conversationMapper.listByUserId(userId).stream()
                .map(c -> new ConversationResponse(c.getId(), c.getTitle(), c.getCreatedAt()))
                .toList();
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
