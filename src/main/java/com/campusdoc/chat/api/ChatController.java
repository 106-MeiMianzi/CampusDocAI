package com.campusdoc.chat.api;

import com.campusdoc.common.ApiResponse;
import com.campusdoc.chat.dto.AskRequest;
import com.campusdoc.chat.dto.AskResponse;
import com.campusdoc.chat.service.ChatService;
import com.campusdoc.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/ask")
    public ApiResponse<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        return ApiResponse.ok(chatService.ask(SecurityUtils.currentUserId(), request));
    }
}
