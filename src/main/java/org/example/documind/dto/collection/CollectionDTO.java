package org.example.documind.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CollectionDTO {
    private Long id;
    private String name;
    private String description;
    private int documentCount;
    private LocalDateTime createdAt;
}
