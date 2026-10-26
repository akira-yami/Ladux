package org.akira.ladux.assistant.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedChatMemoryRepositoryTest {

    private static final List<Message> HELLO = List.of(new UserMessage("xin chao"));

    @Test
    void evictsLeastRecentlyUsedConversationWhenFull() {
        BoundedChatMemoryRepository repo = new BoundedChatMemoryRepository(2, Duration.ofHours(1));
        repo.saveAll("a", HELLO);
        repo.saveAll("b", HELLO);
        repo.findByConversationId("a"); // a vua duoc dung -> b la cu nhat
        repo.saveAll("c", HELLO);

        assertEquals(2, repo.size());
        assertTrue(repo.findByConversationId("b").isEmpty());
        assertEquals(1, repo.findByConversationId("a").size());
        assertEquals(1, repo.findByConversationId("c").size());
    }

    @Test
    void dropsConversationsIdleLongerThanTtl() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-20T00:00:00Z"));
        Clock clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };
        BoundedChatMemoryRepository repo = new BoundedChatMemoryRepository(10, Duration.ofMinutes(30), clock);
        repo.saveAll("idle", HELLO);

        now.set(now.get().plus(Duration.ofMinutes(31)));

        assertTrue(repo.findByConversationId("idle").isEmpty());
        assertTrue(repo.findConversationIds().isEmpty());
    }

    @Test
    void storedHistoryIsDefensivelyCopied() {
        BoundedChatMemoryRepository repo = new BoundedChatMemoryRepository(10, Duration.ofHours(1));
        java.util.ArrayList<Message> messages = new java.util.ArrayList<>(HELLO);
        repo.saveAll("x", messages);
        messages.clear();
        assertEquals(1, repo.findByConversationId("x").size());
    }
}
