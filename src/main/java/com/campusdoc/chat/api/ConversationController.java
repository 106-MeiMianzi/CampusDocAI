package com.campusdoc.chat.api;

import com.campusdoc.common.ApiResponse;
import com.campusdoc.chat.dto.ChatMessageResponse;
import com.campusdoc.chat.dto.ConversationResponse;
import com.campusdoc.chat.dto.CreateConversationRequest;
import com.campusdoc.chat.service.ConversationService;
import com.campusdoc.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ApiResponse<ConversationResponse> create(@RequestBody(required = false) CreateConversationRequest request) {
        if (request == null) {
            request = new CreateConversationRequest();
        }
        return ApiResponse.ok(conversationService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/list")
    public ApiResponse<List<ConversationResponse>> list() {
        return ApiResponse.ok(conversationService.list(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<ChatMessageResponse>> messages(@PathVariable Long id) {
        return ApiResponse.ok(conversationService.messages(SecurityUtils.currentUserId(), id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, String>> delete(@PathVariable Long id) {
        conversationService.delete(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok(Map.of("message", "删除成功"));
    }
}
