package com.campusdoc.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentPreviewTokenResponse {

    private String token;
    private int expiresInSeconds;
}
