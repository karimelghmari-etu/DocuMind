package org.example.documind.web;

import org.example.documind.domain.entity.Document;
import org.example.documind.domain.entity.DocumentChunk;
import org.example.documind.domain.entity.User;
import org.example.documind.domain.enums.DocumentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opening, reading, printing and downloading a document from the browser. */
class DocumentViewTest extends WebTestSupport {

    private static final String TEXT = "Every full-time employee receives 25 days of paid vacation.";

    @Value("${app.storage.location}")
    private String storageLocation;

    private User alice;
    private User bob;
    private Path storedFile;

    @BeforeEach
    void setUp() throws IOException {
        alice = createUser("alice");
        bob = createUser("bob");
        Path dir = Path.of(storageLocation).toAbsolutePath().normalize().resolve(String.valueOf(alice.getId()));
        Files.createDirectories(dir);
        storedFile = dir.resolve("handbook-test.txt");
        Files.writeString(storedFile, TEXT, StandardCharsets.UTF_8);
    }

    @AfterEach
    void deleteFile() throws IOException {
        Files.deleteIfExists(storedFile);
    }

    private Document storedDocument(String contentType, String filename) {
        Document document = documentRepository.save(Document.builder()
                .title("Employee Handbook")
                .filename(filename)
                .filePath(storedFile.toString())
                .contentType(contentType)
                .fileSize((long) TEXT.length())
                .status(DocumentStatus.INDEXED)
                .owner(alice)
                .build());
        chunkRepository.saveAll(List.of(
                DocumentChunk.builder().document(document).chunkIndex(1).content("Second passage.").embedding(new float[384]).build(),
                DocumentChunk.builder().document(document).chunkIndex(0).content(TEXT).embedding(new float[384]).build()));
        return document;
    }

    // ---------------------------------------------------------------- the page

    @Test
    void documentPageShowsTheFileInAViewerWithActions() throws Exception {
        Document doc = storedDocument("text/plain", "handbook.txt");

        mvc.perform(get("/documents/{id}", doc.getId()).with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Employee Handbook")))
                .andExpect(content().string(containsString("<iframe")))
                .andExpect(content().string(containsString("src=\"/documents/" + doc.getId() + "/file\"")))
                .andExpect(content().string(containsString("/documents/" + doc.getId() + "/file?download=true")))
                .andExpect(content().string(containsString("Print")))
                .andExpect(content().string(containsString("Ask about this document")));
    }

    @Test
    void wordFilesAreShownAsTheirIndexedTextInOrder() throws Exception {
        Document doc = storedDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "handbook.docx");

        String html = mvc.perform(get("/documents/{id}", doc.getId()).with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<iframe"))))
                .andExpect(content().string(containsString("Browsers can't display Word files")))
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(
                html.indexOf(TEXT) < html.indexOf("Second passage."), "passages are in document order");
    }

    @Test
    void someoneElsesDocumentPageRedirectsWithAMessage() throws Exception {
        Document doc = storedDocument("text/plain", "handbook.txt");

        mvc.perform(get("/documents/{id}", doc.getId()).with(as(bob)))
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attribute("errorMessage", "This document no longer exists."));
    }

    @Test
    void documentCardsLinkToTheDocumentPage() throws Exception {
        Document doc = storedDocument("text/plain", "handbook.txt");

        mvc.perform(get("/documents").with(as(alice)))
                .andExpect(content().string(containsString("href=\"/documents/" + doc.getId() + "\"")));
    }

    // ---------------------------------------------------------------- the file

    @Test
    void fileOpensInlineForReadingAndPrinting() throws Exception {
        Document doc = storedDocument("text/plain", "handbook.txt");

        mvc.perform(get("/documents/{id}/file", doc.getId()).with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("text/plain")))
                .andExpect(header().string("Content-Disposition", startsWith("inline")))
                .andExpect(header().string("Content-Disposition", containsString("handbook.txt")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().string(TEXT));
    }

    @Test
    void downloadAsksTheBrowserToSaveTheFile() throws Exception {
        Document doc = storedDocument("text/plain", "handbook.txt");

        mvc.perform(get("/documents/{id}/file", doc.getId()).param("download", "true").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", startsWith("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("handbook.txt")));
    }

    @Test
    void wordFilesAreAlwaysDownloadedNeverShownInline() throws Exception {
        Document doc = storedDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "handbook.docx");

        mvc.perform(get("/documents/{id}/file", doc.getId()).with(as(alice)))
                .andExpect(header().string("Content-Disposition", startsWith("attachment")));
    }

    @Test
    void someoneElsesFileIs404() throws Exception {
        Document doc = storedDocument("text/plain", "handbook.txt");

        mvc.perform(get("/documents/{id}/file", doc.getId()).with(as(bob)))
                .andExpect(status().isNotFound());
    }

    @Test
    void notLoggedInCannotGetTheFile() throws Exception {
        Document doc = storedDocument("text/plain", "handbook.txt");

        mvc.perform(get("/documents/{id}/file", doc.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void aStoredPathOutsideTheStorageFolderIsRefused() throws Exception {
        Document doc = documentRepository.save(Document.builder()
                .title("Sneaky")
                .filename("hosts")
                .filePath(Path.of(System.getProperty("java.io.tmpdir"), "not-in-storage.txt").toAbsolutePath().toString())
                .contentType("text/plain")
                .fileSize(1L)
                .status(DocumentStatus.INDEXED)
                .owner(alice)
                .build());

        mvc.perform(get("/documents/{id}/file", doc.getId()).with(as(alice)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("not-in-storage"))));
    }

    // ---------------------------------------------------------------- the sidebar

    @Test
    void sidebarListsMyCollectionsOnEveryPage() throws Exception {
        collectionRepository.save(org.example.documind.domain.entity.Collection.builder().name("Machine learning").owner(alice).build());
        collectionRepository.save(org.example.documind.domain.entity.Collection.builder().name("Bob only").owner(bob).build());

        mvc.perform(get("/history").with(as(alice)))
                .andExpect(content().string(containsString("Machine learning")))
                .andExpect(content().string(not(containsString("Bob only"))));
    }
}
