package org.example.documind.dto.user;

import java.time.LocalDateTime;

/** What the Settings page shows about the logged-in user. Never contains the password. */
public record AccountDTO(Long id, String username, String email, LocalDateTime createdAt) {}
