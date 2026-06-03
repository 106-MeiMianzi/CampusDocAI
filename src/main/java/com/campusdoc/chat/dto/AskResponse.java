package com.campusdoc.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskResponse {

    private Long conversationId;
    private String answer;
    private List<CitationResponse> citations;
    private String suggestion;
}
