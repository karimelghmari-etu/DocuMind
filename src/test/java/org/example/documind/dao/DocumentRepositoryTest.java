package org.example.documind.dao;

import org.example.documind.domain.entity.Document;
import org.example.documind.domain.entity.User;
import org.example.documind.domain.enums.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testDocumentOwnershipAndFiltering() {
        User owner = User.builder()
                .username("docowner")
                .email("owner@example.com")
                .password("password")
                .build();
        userRepository.save(owner);

        Document doc = Document.builder()
                .title("Architecture Notes")
                .filename("architecture.pdf")
                .filePath("/uploads/architecture.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .status(DocumentStatus.INDEXED)
                .owner(owner)
                .build();

        documentRepository.save(doc);

        List<Document> ownerDocs = documentRepository.findByOwnerId(owner.getId());
        assertThat(ownerDocs).hasSize(1);
        assertThat(ownerDocs.get(0).getTitle()).isEqualTo("Architecture Notes");
        assertThat(ownerDocs.get(0).getStatus()).isEqualTo(DocumentStatus.INDEXED);
    }
}