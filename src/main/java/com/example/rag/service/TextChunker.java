package com.example.rag.service;

import com.example.rag.config.RagProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TextChunker {

    private final RagProperties properties;

    public TextChunker(RagProperties properties) {
        this.properties = properties;
    }

    public List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String cleaned = paragraph.trim();
            if (cleaned.isEmpty()) {
                continue;
            }

            if (cleaned.length() > properties.chunkSize()) {
                flushCurrentChunk(chunks, current);
                chunks.addAll(splitFixedWindows(cleaned));
                continue;
            }

            if (current.length() + cleaned.length() + 2 > properties.chunkSize() && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                current = new StringBuilder(overlapTail(current.toString()));
            }

            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(cleaned);
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    private void flushCurrentChunk(List<String> chunks, StringBuilder current) {
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
            current.setLength(0);
        }
    }

    private List<String> splitFixedWindows(String text) {
        List<String> windows = new ArrayList<>();
        int chunkSize = properties.chunkSize();
        int overlap = Math.min(properties.chunkOverlap(), chunkSize - 1);
        int step = Math.max(1, chunkSize - overlap);
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(text.length(), start + chunkSize);
            windows.add(text.substring(start, end).trim());
            if (end >= text.length()) {
                break;
            }
        }
        return windows;
    }

    private String overlapTail(String text) {
        int overlap = Math.max(0, properties.chunkOverlap());
        if (overlap == 0 || text.length() <= overlap) {
            return "";
        }
        return text.substring(text.length() - overlap).trim();
    }
}
