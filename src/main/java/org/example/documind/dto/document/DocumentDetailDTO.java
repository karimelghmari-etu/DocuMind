package org.example.documind.dto.document;

import org.example.documind.domain.enums.DocumentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the document page shows: file details, status, the collections it is in,
 * and its indexed passages (used to read Word files, which browsers can't display).
 */
public record DocumentDetailDTO(
        Long id,
        String title,
        String filename,
        String contentType,
        long fileSize,
        DocumentStatus status,
        LocalDateTime uploadedAt,
        List<CollectionRef> collections,
        List<Passage> passages
) {
    public record CollectionRef(Long id, String name) {}

    public record Passage(int index, String content) {}

    /** PDF and plain text open in the browser's own viewer; Word files do not. */
    public boolean viewableInBrowser() {
        return "application/pdf".equals(contentType) || "text/plain".equals(contentType);
    }

    /** "PDF", "DOCX", "TXT": a short label for the file type. */
    public String typeLabel() {
        if ("application/pdf".equals(contentType)) return "PDF";
        if ("text/plain".equals(contentType)) return "TXT";
        if (contentType != null && contentType.contains("wordprocessingml")) return "DOCX";
        return "FILE";
    }
}
