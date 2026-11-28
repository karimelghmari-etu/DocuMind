package org.example.documind.web.controller;

import org.example.documind.dto.chat.ConversationDTO;
import org.example.documind.dto.document.DocumentDTO;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * One line of the dashboard's "Recent activity": an upload or a conversation,
 * with a link and a short "2 h ago" time.
 */
public record Activity(String kind, String text, String href, LocalDateTime at, String when) {

    static final int MAX = 8;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    static List<Activity> from(List<DocumentDTO> documents, List<ConversationDTO> conversations, LocalDateTime now) {
        List<Activity> items = new ArrayList<>();
        for (DocumentDTO d : documents) {
            if (d.getUploadedAt() != null) {
                items.add(new Activity("upload", d.getTitle(), "/documents/" + d.getId(), d.getUploadedAt(),
                        timeAgo(d.getUploadedAt(), now)));
            }
        }
        for (ConversationDTO c : conversations) {
            if (c.createdAt() != null) {
                items.add(new Activity("chat", c.title(), "/assistant?conversation=" + c.id(), c.createdAt(),
                        timeAgo(c.createdAt(), now)));
            }
        }
        items.sort(Comparator.comparing(Activity::at).reversed());
        return items.size() > MAX ? items.subList(0, MAX) : items;
    }

    /** "just now", "5 min ago", "3 h ago", "yesterday", "4 days ago", then the date. */
    static String timeAgo(LocalDateTime then, LocalDateTime now) {
        Duration d = Duration.between(then, now);
        if (d.isNegative() || d.toMinutes() < 1) return "just now";
        if (d.toMinutes() < 60) return d.toMinutes() + " min ago";
        if (d.toHours() < 24) return d.toHours() + " h ago";
        long days = d.toDays();
        if (days == 1) return "yesterday";
        if (days < 7) return days + " days ago";
        return then.format(DATE);
    }
}
