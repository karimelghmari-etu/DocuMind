package org.example.documind.web;

import org.example.documind.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SettingsWebControllerTest extends WebTestSupport {

    private static final String NEW_PASSWORD = "brand-new-pass-2";

    private User alice;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
    }

    private MockHttpServletRequestBuilder changePassword(String current, String next, String confirm) {
        return post("/settings/password").with(as(alice)).with(csrf())
                .param("currentPassword", current)
                .param("newPassword", next)
                .param("confirmPassword", confirm);
    }

    private String storedHash() {
        return userRepository.findById(alice.getId()).orElseThrow().getPassword();
    }

    @Test
    void pageShowsAccountDetailsButNeverThePassword() throws Exception {
        mvc.perform(get("/settings").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(alice.getUsername())))
                .andExpect(content().string(containsString(alice.getEmail())))
                .andExpect(content().string(not(containsString(storedHash()))));
    }

    @Test
    void changePasswordWorks() throws Exception {
        mvc.perform(changePassword(PASSWORD, NEW_PASSWORD, NEW_PASSWORD))
                .andExpect(redirectedUrl("/settings"))
                .andExpect(flash().attribute("successMessage", "Your password was changed."));

        assertTrue(passwordEncoder.matches(NEW_PASSWORD, storedHash()));
        assertFalse(passwordEncoder.matches(PASSWORD, storedHash()));
    }

    @Test
    void wrongCurrentPasswordIsAFieldError() throws Exception {
        String before = storedHash();

        mvc.perform(changePassword("not-my-password", NEW_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("passwordForm", "currentPassword"))
                .andExpect(content().string(containsString("Current password is incorrect")));

        assertEquals(before, storedHash());
    }

    @Test
    void mismatchedConfirmationIsAFieldError() throws Exception {
        mvc.perform(changePassword(PASSWORD, NEW_PASSWORD, "something-else"))
                .andExpect(model().attributeHasFieldErrors("passwordForm", "confirmPassword"))
                .andExpect(content().string(containsString("The two passwords do not match")));

        assertTrue(passwordEncoder.matches(PASSWORD, storedHash()));
    }

    @Test
    void tooShortNewPasswordIsAFieldError() throws Exception {
        mvc.perform(changePassword(PASSWORD, "short", "short"))
                .andExpect(model().attributeHasFieldErrors("passwordForm", "newPassword"))
                .andExpect(content().string(containsString("at least 8 characters")));

        assertTrue(passwordEncoder.matches(PASSWORD, storedHash()));
    }

    @Test
    void typedPasswordsAreNeverSentBackToTheBrowser() throws Exception {
        mvc.perform(changePassword("wrong-but-secret-123", "short", "short"))
                .andExpect(content().string(not(containsString("wrong-but-secret-123"))));
    }

    @Test
    void changePasswordWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/settings/password").with(as(alice))
                        .param("currentPassword", PASSWORD)
                        .param("newPassword", NEW_PASSWORD)
                        .param("confirmPassword", NEW_PASSWORD))
                .andExpect(status().isForbidden());

        assertTrue(passwordEncoder.matches(PASSWORD, storedHash()));
    }
}
