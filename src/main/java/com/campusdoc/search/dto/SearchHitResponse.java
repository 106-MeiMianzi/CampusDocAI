package com.campusdoc.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchHitResponse {

    private Long chunkId;
    private Long documentId;
    private String document;
    private Double score;
    private String content;
}
