package com.campusdoc.search.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequest {

    @NotBlank(message = "检索内容不能为空")
    @JsonAlias({"query", "content", "text"})
    private String question;

    private String keyword;

    private Integer topK;
}
