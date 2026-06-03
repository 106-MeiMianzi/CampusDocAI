package com.campusdoc.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private String role;
    private String content;
    private List<CitationResponse> citations;
    private LocalDateTime createdAt;
}
