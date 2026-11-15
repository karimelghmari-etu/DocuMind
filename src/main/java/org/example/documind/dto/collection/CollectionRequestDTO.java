package org.example.documind.dto.collection;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollectionRequestDTO {
    @NotBlank
    private String name;
    private String description;
}
