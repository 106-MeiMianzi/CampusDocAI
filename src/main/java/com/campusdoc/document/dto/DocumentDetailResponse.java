package com.campusdoc.document.dto;

import com.campusdoc.document.entity.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DocumentDetailResponse {

    private Long id;
    private String fileName;
    private DocumentStatus status;
    private Long fileSize;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
