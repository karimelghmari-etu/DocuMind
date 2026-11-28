package org.example.documind.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.example.documind.domain.entity.Document;
import org.example.documind.domain.enums.DocumentStatus;

import java.time.LocalDateTime;
@Setter
@Getter
public class DocumentDTO {
    private Long id;
    private String title;
    private DocumentStatus status;
    private LocalDateTime uploadedAt;
    /** Added for the web pages (file type badge and size); null when built with the short constructor. */
    private String contentType;
    private Long fileSize;

    public DocumentDTO(Long id, String title, DocumentStatus status, LocalDateTime uploadedAt) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.uploadedAt = uploadedAt;
    }

    public DocumentDTO(Long id, String title, DocumentStatus status, LocalDateTime uploadedAt,
                       String contentType, Long fileSize) {
        this(id, title, status, uploadedAt);
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    /** "PDF", "DOCX", "TXT" for the badge on a document card. Not part of the API's JSON. */
    @JsonIgnore
    public String getTypeLabel() {
        if ("application/pdf".equals(contentType)) return "PDF";
        if ("text/plain".equals(contentType)) return "TXT";
        if (contentType != null && contentType.contains("wordprocessingml")) return "DOCX";
        return "FILE";
    }


}
