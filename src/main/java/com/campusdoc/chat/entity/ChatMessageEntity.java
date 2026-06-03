package com.campusdoc.chat.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageEntity {

    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private String citationsJson;
    private LocalDateTime createdAt;
}
