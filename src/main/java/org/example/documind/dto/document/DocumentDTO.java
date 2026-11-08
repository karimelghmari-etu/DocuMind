package org.example.documind.dto.document;

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
    public DocumentDTO(Long id, String title, DocumentStatus status, LocalDateTime uploadedAt) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.uploadedAt = uploadedAt;
    }


}
