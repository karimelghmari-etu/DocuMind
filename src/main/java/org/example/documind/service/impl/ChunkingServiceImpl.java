package org.example.documind.service.impl;

import org.example.documind.service.interfaces.ChunkingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingServiceImpl implements ChunkingService {

    private final int chunkSize;
    private final int overlap;

    public ChunkingServiceImpl(
            @Value("${app.chunking.size}") int chunkSize,
            @Value("${app.chunking.overlap}") int overlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than 0");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException("Overlap cannot be negative");
        }
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("Overlap must be less than chunk size");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    @Override
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        text = text.replaceAll("\\n{3,}", "\n\n");

        if (text.length() <= chunkSize) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int textLength = text.length();

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);

            if (end < textLength) {
                int lastSpace = text.lastIndexOf(' ', end);
                int lastNewline = text.lastIndexOf('\n', end);
                int splitIndex = Math.max(lastSpace, lastNewline);

                int midpoint = start + (chunkSize / 2);
                if (splitIndex > midpoint) {
                    end = splitIndex;
                }
            }

            String chunkText = text.substring(start, end).strip();
            if (!chunkText.isBlank()) {
                chunks.add(chunkText);
            }

            if (end >= textLength) {
                break;
            }

            int oldStart = start;
            start = end - overlap;

            if (start > 0 && start < textLength && !Character.isWhitespace(text.charAt(start - 1))) {
                int nextSpace = text.indexOf(' ', start);
                int nextNewline = text.indexOf('\n', start);

                int nextWhitespace = -1;
                if (nextSpace != -1 && nextNewline != -1) {
                    nextWhitespace = Math.min(nextSpace, nextNewline);
                } else if (nextSpace != -1) {
                    nextWhitespace = nextSpace;
                } else {
                    nextWhitespace = nextNewline;
                }

                if (nextWhitespace != -1 && nextWhitespace < end) {
                    start = nextWhitespace + 1;
                }
            }

            if (start <= oldStart) {
                start = end;
            }
        }

        return chunks;
    }
}