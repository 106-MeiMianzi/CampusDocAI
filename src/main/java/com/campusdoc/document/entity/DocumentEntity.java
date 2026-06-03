package com.campusdoc.document.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentEntity {

    private Long id;
    private Long userId;
    private String fileName;
    private String storagePath;
    private Long fileSize;
    private DocumentStatus status;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
