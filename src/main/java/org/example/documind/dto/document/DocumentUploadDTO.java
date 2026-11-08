package org.example.documind.dto.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter

public class DocumentUploadDTO {
    private String title;
    private MultipartFile file;
}
