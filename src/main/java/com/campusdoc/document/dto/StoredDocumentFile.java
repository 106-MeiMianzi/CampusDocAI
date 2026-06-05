package com.campusdoc.document.dto;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record StoredDocumentFile(Resource resource, MediaType mediaType, String fileName) {
}
