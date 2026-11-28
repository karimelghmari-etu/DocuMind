package org.example.documind.web;

import org.example.documind.dao.DocumentChunkRepository;
import org.example.documind.dao.DocumentRepository;
import org.example.documind.dao.ProcessingJobRepository;
import org.example.documind.dao.UserRepository;
import org.example.documind.domain.entity.Document;
import org.example.documind.domain.entity.ProcessingJob;
import org.example.documind.domain.entity.User;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.domain.enums.ProcessingStatus;
import org.example.documind.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The browser pages for documents: real Spring context, real security chain, real H2.
 * Errors must come back as a flash message on /documents, never as a raw JSON page.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentsWebControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentChunkRepository chunkRepository;
    @Autowired private ProcessingJobRepository jobRepository;

    private User alice;
    private User bob;
    private Document alicesDocument;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
        bob = createUser("bob");
        alicesDocument = createDocument(alice, DocumentStatus.PROCESSING);
    }

    @AfterEach
    void cleanUp() {
        jobRepository.deleteAll();
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll(List.of(alice, bob));
    }

    // ---------- helpers ----------

    private User createUser(String prefix) {
        String name = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .username(name)
                .email(name + "@example.com")
                .password("irrelevant")
                .build());
    }

    private Document createDocument(User owner, DocumentStatus status) {
        return documentRepository.save(Document.builder()
                .title("Handbook")
                .filename("handbook.txt")
                .filePath("/not/used/handbook.txt")
                .contentType("text/plain")
                .fileSize(1L)
                .status(status)
                .owner(owner)
                .build());
    }

    private static RequestPostProcessor as(User user) {
        return user(new CustomUserDetails(user));
    }

    private static MockMultipartFile file(String name, String contentType, String content) {
        return new MockMultipartFile("file", name, contentType, content.getBytes());
    }

    private List<Document> documentsOf(User user) {
        return documentRepository.findByOwnerId(user.getId());
    }

    // ---------------------------------------------------------------- page

    @Test
    void notLoggedInGoesToTheLoginPage() throws Exception {
        mvc.perform(get("/documents"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void pageShowsAnErrorFlashMessage() throws Exception {
        mvc.perform(get("/documents").with(as(alice)).flashAttr("errorMessage", "Unsupported file type"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"error-message\"")))
                .andExpect(content().string(containsString("Unsupported file type")));
    }

    @Test
    void badgesCarryWhatThePollingScriptNeeds() throws Exception {
        mvc.perform(get("/documents").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-document-id=\"" + alicesDocument.getId() + "\"")))
                .andExpect(content().string(containsString("data-status=\"PROCESSING\"")))
                .andExpect(content().string(containsString("data-max-size=\"")));
    }

    // ---------------------------------------------------------------- upload

    @Test
    void validUploadRedirectsWithASuccessMessage() throws Exception {
        mvc.perform(multipart("/documents/upload")
                        .file(file("notes.txt", "text/plain", "Some notes."))
                        .param("title", "My notes")
                        .with(as(bob)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attribute("successMessage", "Document uploaded successfully!"));

        List<Document> bobs = documentsOf(bob);
        assertEquals(1, bobs.size());
        assertEquals("My notes", bobs.get(0).getTitle());
    }

    @Test
    void unsupportedFileTypeIsAFlashMessageNotJson() throws Exception {
        mvc.perform(multipart("/documents/upload")
                        .file(file("virus.exe", "application/x-msdownload", "MZ"))
                        .with(as(bob)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attribute("errorMessage", "Unsupported file type"));

        assertTrue(documentsOf(bob).isEmpty());
    }

    @Test
    void emptyFileIsAFlashMessage() throws Exception {
        mvc.perform(multipart("/documents/upload")
                        .file(file("empty.txt", "text/plain", ""))
                        .with(as(bob)).with(csrf()))
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attribute("errorMessage", "File is required"));
    }

    @Test
    void tooLongTitleIsAFlashMessage() throws Exception {
        mvc.perform(multipart("/documents/upload")
                        .file(file("notes.txt", "text/plain", "Some notes."))
                        .param("title", "x".repeat(256))
                        .with(as(bob)).with(csrf()))
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attribute("errorMessage", "The title must be at most 255 characters."));

        assertTrue(documentsOf(bob).isEmpty());
    }

    @Test
    void uploadWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(multipart("/documents/upload")
                        .file(file("notes.txt", "text/plain", "Some notes."))
                        .with(as(bob)))
                .andExpect(status().isForbidden());

        assertTrue(documentsOf(bob).isEmpty());
    }

    // ---------------------------------------------------------------- delete / reprocess

    @Test
    void cannotDeleteSomeoneElsesDocument() throws Exception {
        mvc.perform(post("/documents/{id}/delete", alicesDocument.getId()).with(as(bob)).with(csrf()))
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attribute("errorMessage", "This document no longer exists."));

        assertTrue(documentRepository.existsById(alicesDocument.getId()));
    }

    @Test
    void reprocessingADocumentThatDidNotFailIsAFlashMessage() throws Exception {
        jobRepository.save(ProcessingJob.builder()
                .document(alicesDocument)
                .status(ProcessingStatus.EMBEDDING)
                .build());

        mvc.perform(post("/documents/{id}/reprocess", alicesDocument.getId()).with(as(alice)).with(csrf()))
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attribute("errorMessage", "Only failed documents can be reprocessed."));
    }

    // ---------------------------------------------------------------- status polling

    @Test
    void statusEndpointReturnsTheCurrentStatusAsJson() throws Exception {
        mvc.perform(get("/documents/{id}/status", alicesDocument.getId()).with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(alicesDocument.getId()))
                .andExpect(jsonPath("$.documentStatus").value("PROCESSING"));
    }

    @Test
    void statusOfSomeoneElsesDocumentIs404() throws Exception {
        mvc.perform(get("/documents/{id}/status", alicesDocument.getId()).with(as(bob)))
                .andExpect(status().isNotFound());
    }
}
