package com.campusdoc.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentUploadItemResponse {

    private Long docId;
    private String fileName;
}
