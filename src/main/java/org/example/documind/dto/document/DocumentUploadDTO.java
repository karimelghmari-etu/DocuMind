package org.example.documind.dto.document;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
public class DocumentUploadDTO {
    @Size(max = 255)
    private String title;
    private MultipartFile file;
}
