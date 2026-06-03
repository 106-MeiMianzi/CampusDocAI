package com.campusdoc.document.entity;

import lombok.Data;

@Data
public class DocChunkVectorRow {

    private Long id;
    private Long documentId;
    private Long userId;
    private Integer chunkIndex;
    private String content;
    private String fileName;
    private String uploaderRole;
}
