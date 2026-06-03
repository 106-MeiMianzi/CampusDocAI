package com.campusdoc.chat.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationEntity {

    private Long id;
    private Long userId;
    private String title;
    private LocalDateTime createdAt;
}
