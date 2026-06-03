package com.campusdoc.document;

import com.campusdoc.config.DocumentProperties;
import com.campusdoc.document.service.TextChunker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    @Test
    void chunksWithConfiguredStep() {
        DocumentProperties props = new DocumentProperties();
        props.setChunkSize(600);
        props.setChunkStep(500);
        TextChunker chunker = new TextChunker(props);
        String text = "a".repeat(1200);
        List<String> chunks = chunker.chunk(text);
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks.get(0)).hasSize(600);
    }
}
