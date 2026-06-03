package com.campusdoc.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AskRequest {

    @NotBlank(message = "问题不能为空")
    private String question;

    private Long conversationId;

    /** 非空时仅在该文档的切块范围内检索 */
    private Long documentId;
}
