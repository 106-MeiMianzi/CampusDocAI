package com.campusdoc.document.service;

import com.campusdoc.config.DocumentProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final DocumentProperties documentProperties;

    public TextChunker(DocumentProperties documentProperties) {
        this.documentProperties = documentProperties;
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int size = documentProperties.getChunkSize();
        int step = documentProperties.getChunkStep();
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(i + size, text.length());
            String part = text.substring(i, end).trim();
            if (!part.isEmpty()) {
                chunks.add(part);
            }
            if (end >= text.length()) {
                break;
            }
        }
        return chunks;
    }
}
