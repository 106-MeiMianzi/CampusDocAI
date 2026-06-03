package com.campusdoc.document.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocChunkEntity {

    private Long id;
    private Long documentId;
    private Long userId;
    private int chunkIndex;
    private String content;
    private LocalDateTime createdAt;
}
