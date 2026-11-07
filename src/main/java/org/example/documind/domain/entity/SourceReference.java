package org.example.documind.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "source_references")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", nullable = false)
    private DocumentChunk chunk;

    @Column(nullable = false)
    private Double relevanceScore;
}