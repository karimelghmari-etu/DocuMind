package org.example.documind.dto.document;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentUpdateDTO {
    @NotBlank
    private String title;
}

