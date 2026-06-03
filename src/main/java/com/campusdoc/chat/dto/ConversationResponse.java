package com.campusdoc.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ConversationResponse {

    private Long id;
    private String title;
    private LocalDateTime createdAt;
}
