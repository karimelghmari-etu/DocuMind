package org.example.documind.web;

import org.example.documind.domain.entity.Collection;
import org.example.documind.domain.entity.Document;
import org.example.documind.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CollectionsWebControllerTest extends WebTestSupport {

    @Autowired private TransactionTemplate transactionTemplate;

    private User alice;
    private User bob;
    private Document handbook;
    private Document policy;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
        bob = createUser("bob");
        handbook = createDocument(alice, "Handbook");
        policy = createDocument(alice, "Travel policy");
    }

    private Collection createCollection(User owner, String name) {
        return collectionRepository.save(Collection.builder().name(name).owner(owner).build());
    }

    /** Document IDs in a collection, read in a transaction because the relation is lazy. */
    private Set<Long> documentIdsOf(Collection collection) {
        return transactionTemplate.execute(status -> collectionRepository.findById(collection.getId()).orElseThrow()
                .getDocuments().stream().map(Document::getId).collect(Collectors.toSet()));
    }

    // ---------------------------------------------------------------- list and create

    @Test
    void listShowsOnlyMyCollections() throws Exception {
        createCollection(alice, "Alice's course");
        createCollection(bob, "Bob's secret project");

        mvc.perform(get("/collections").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Alice&#39;s course")))
                .andExpect(content().string(not(containsString("Bob&#39;s secret project"))));
    }

    @Test
    void createRedirectsToTheNewCollection() throws Exception {
        mvc.perform(post("/collections").with(as(alice)).with(csrf())
                        .param("name", "  Machine Learning  ")
                        .param("description", "Course notes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/collections/*"))
                .andExpect(flash().attributeExists("successMessage"));

        Collection saved = collectionRepository.findByOwnerId(alice.getId()).get(0);
        assertEquals("Machine Learning", saved.getName(), "the name is trimmed");
        assertEquals("Course notes", saved.getDescription());
    }

    @Test
    void blankNameIsAFlashMessage() throws Exception {
        mvc.perform(post("/collections").with(as(alice)).with(csrf()).param("name", "   "))
                .andExpect(redirectedUrl("/collections"))
                .andExpect(flash().attribute("errorMessage", "Please give the collection a name."));

        assertTrue(collectionRepository.findByOwnerId(alice.getId()).isEmpty());
    }

    @Test
    void tooLongNameIsAFlashMessage() throws Exception {
        mvc.perform(post("/collections").with(as(alice)).with(csrf()).param("name", "x".repeat(101)))
                .andExpect(redirectedUrl("/collections"))
                .andExpect(flash().attribute("errorMessage", "The name must be at most 100 characters."));
    }

    @Test
    void createWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/collections").with(as(alice)).param("name", "No token"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- detail

    @Test
    void detailSplitsDocumentsInAndOutOfTheCollection() throws Exception {
        Collection course = createCollection(alice, "Course");
        mvc.perform(post("/collections/{id}/documents", course.getId()).with(as(alice)).with(csrf())
                        .param("documentId", handbook.getId().toString()))
                .andExpect(redirectedUrl("/collections/" + course.getId()));

        mvc.perform(get("/collections/{id}", course.getId()).with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("documents", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(model().attribute("otherDocuments", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(content().string(containsString("Travel policy")));   // offered in the "add" list
    }

    @Test
    void someoneElsesCollectionRedirectsWithAMessage() throws Exception {
        Collection alices = createCollection(alice, "Private");

        mvc.perform(get("/collections/{id}", alices.getId()).with(as(bob)))
                .andExpect(redirectedUrl("/collections"))
                .andExpect(flash().attribute("errorMessage", "This collection no longer exists."));
    }

    @Test
    void addAndRemoveDocument() throws Exception {
        Collection course = createCollection(alice, "Course");

        mvc.perform(post("/collections/{id}/documents", course.getId()).with(as(alice)).with(csrf())
                .param("documentId", policy.getId().toString()));
        assertEquals(Set.of(policy.getId()), documentIdsOf(course));

        mvc.perform(post("/collections/{cid}/documents/{did}/remove", course.getId(), policy.getId())
                        .with(as(alice)).with(csrf()))
                .andExpect(flash().attributeExists("successMessage"));
        assertEquals(Set.of(), documentIdsOf(course));
        assertTrue(documentRepository.existsById(policy.getId()), "removing from a collection never deletes the document");
    }

    @Test
    void cannotAddSomeoneElsesDocument() throws Exception {
        Collection bobs = createCollection(bob, "Bob's");

        mvc.perform(post("/collections/{id}/documents", bobs.getId()).with(as(bob)).with(csrf())
                        .param("documentId", handbook.getId().toString()))
                .andExpect(flash().attribute("errorMessage", "This document no longer exists."));

        assertEquals(Set.of(), documentIdsOf(bobs));
    }

    @Test
    void addWithoutChoosingADocumentIsAFlashMessage() throws Exception {
        Collection course = createCollection(alice, "Course");

        mvc.perform(post("/collections/{id}/documents", course.getId()).with(as(alice)).with(csrf()))
                .andExpect(flash().attribute("errorMessage", "Choose a document to add."));
    }

    @Test
    void renameCollection() throws Exception {
        Collection course = createCollection(alice, "Old name");

        mvc.perform(post("/collections/{id}", course.getId()).with(as(alice)).with(csrf())
                        .param("name", "New name").param("description", ""))
                .andExpect(redirectedUrl("/collections/" + course.getId()));

        Collection saved = collectionRepository.findById(course.getId()).orElseThrow();
        assertEquals("New name", saved.getName());
        assertNull(saved.getDescription(), "an empty description is stored as null");
    }

    @Test
    void deleteCollectionKeepsItsDocuments() throws Exception {
        Collection course = createCollection(alice, "Course");
        mvc.perform(post("/collections/{id}/documents", course.getId()).with(as(alice)).with(csrf())
                .param("documentId", handbook.getId().toString()));

        mvc.perform(post("/collections/{id}/delete", course.getId()).with(as(alice)).with(csrf()))
                .andExpect(redirectedUrl("/collections"));

        assertFalse(collectionRepository.existsById(course.getId()));
        assertTrue(documentRepository.existsById(handbook.getId()));
    }

    @Test
    void cannotDeleteSomeoneElsesCollection() throws Exception {
        Collection alices = createCollection(alice, "Private");

        mvc.perform(post("/collections/{id}/delete", alices.getId()).with(as(bob)).with(csrf()))
                .andExpect(flash().attribute("errorMessage", "This collection no longer exists."));

        assertTrue(collectionRepository.existsById(alices.getId()));
    }
}
