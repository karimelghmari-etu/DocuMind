package org.example.documind.web;

import org.example.documind.domain.entity.Conversation;
import org.example.documind.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** History and Search pages, plus the shared sidebar and navbar they render. */
class WorkspaceWebControllerTest extends WebTestSupport {

    private static final String VACATION =
            "Every full-time employee receives 25 days of paid vacation each calendar year.";

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
        bob = createUser("bob");
        createDocument(alice, "Employee Handbook", VACATION);
    }

    private Conversation createConversation(User user, String title) {
        return conversationRepository.save(Conversation.builder().title(title).user(user).build());
    }

    // ---------------------------------------------------------------- layout

    @Test
    void sidebarLinksAreRealAndNavbarSearchIsEnabled() throws Exception {
        mvc.perform(get("/history").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/collections\"")))
                .andExpect(content().string(containsString("href=\"/history\"")))
                .andExpect(content().string(containsString("href=\"/settings\"")))
                .andExpect(content().string(not(containsString("coming soon"))))
                .andExpect(content().string(containsString("action=\"/search\"")))
                .andExpect(content().string(not(containsString("disabled>"))));
    }

    @Test
    void everyPageRenders() throws Exception {
        for (String page : new String[]{"/dashboard", "/documents", "/collections", "/assistant",
                "/history", "/search", "/settings"}) {
            mvc.perform(get(page).with(as(alice)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("class=\"sidebar\"")));
        }
    }

    // ---------------------------------------------------------------- history

    @Test
    void historyListsOnlyMyConversations() throws Exception {
        createConversation(alice, "Vacation questions");
        createConversation(bob, "Bob's private chat");

        mvc.perform(get("/history").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vacation questions")))
                .andExpect(content().string(not(containsString("Bob&#39;s private chat"))));
    }

    @Test
    void historyLinksOpenTheConversationInTheAssistant() throws Exception {
        Conversation chat = createConversation(alice, "Vacation questions");

        mvc.perform(get("/history").with(as(alice)))
                .andExpect(content().string(containsString("href=\"/assistant?conversation=" + chat.getId() + "\"")));
    }

    @Test
    void deleteFromHistory() throws Exception {
        Conversation chat = createConversation(alice, "To delete");

        mvc.perform(post("/history/{id}/delete", chat.getId()).with(as(alice)).with(csrf()))
                .andExpect(redirectedUrl("/history"))
                .andExpect(flash().attribute("successMessage", "Conversation deleted."));

        assertFalse(conversationRepository.existsById(chat.getId()));
    }

    @Test
    void cannotDeleteSomeoneElsesConversation() throws Exception {
        Conversation chat = createConversation(alice, "Private");

        mvc.perform(post("/history/{id}/delete", chat.getId()).with(as(bob)).with(csrf()))
                .andExpect(flash().attribute("errorMessage", "This conversation no longer exists."));

        assertTrue(conversationRepository.existsById(chat.getId()));
    }

    // ---------------------------------------------------------------- search

    @Test
    void searchWithoutQueryShowsTheHint() throws Exception {
        mvc.perform(get("/search").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("results"))
                .andExpect(content().string(containsString("Search your documents")));
    }

    @Test
    void searchFindsMyPassageByMeaning() throws Exception {
        mvc.perform(get("/search").param("q", "How much time off do I get?").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("results", hasSize(1)))
                .andExpect(content().string(containsString("Employee Handbook")))
                .andExpect(content().string(containsString("25 days of paid vacation")))
                .andExpect(content().string(containsString("value=\"How much time off do I get?\"")));
    }

    @Test
    void searchNeverShowsSomeoneElsesDocuments() throws Exception {
        mvc.perform(get("/search").param("q", "How many vacation days?").with(as(bob)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("results", empty()))
                .andExpect(content().string(not(containsString("25 days of paid vacation"))))
                .andExpect(content().string(containsString("No relevant passages found")));
    }

    @Test
    void unrelatedSearchShowsNoWeakMatches() throws Exception {
        mvc.perform(get("/search").param("q", "How do rockets reach orbit?").with(as(alice)))
                .andExpect(model().attribute("results", empty()));
    }

    @Test
    void tooLongSearchIsAnErrorMessage() throws Exception {
        mvc.perform(get("/search").param("q", "x".repeat(501)).with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "Your search must be at most 500 characters."));
    }

    @Test
    void searchEscapesTheQuery() throws Exception {
        mvc.perform(get("/search").param("q", "<script>alert(1)</script> vacation").with(as(alice)))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }
}
