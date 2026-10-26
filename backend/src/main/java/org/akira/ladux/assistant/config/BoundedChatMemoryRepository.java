package org.akira.ladux.assistant.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

/**
 * Chat memory trong RAM co gioi han, thay cho InMemoryChatMemoryRepository mac dinh
 * (ConcurrentHashMap khong gioi han so hoi thoai, khong TTL).
 *
 * <p>conversationId do client gui len tren endpoint public, nen khong gioi han se cho phep
 * ke tan cong tao vo so hoi thoai va lam can heap. Repository nay:
 * <ul>
 *   <li>giu toi da {@code maxConversations} hoi thoai, loai bo hoi thoai it dung nhat (LRU);</li>
 *   <li>xoa hoi thoai khong duoc truy cap qua {@code ttl}.</li>
 * </ul>
 */
public class BoundedChatMemoryRepository implements ChatMemoryRepository {

    private final int maxConversations;
    private final Duration ttl;
    private final Clock clock;
    private final LinkedHashMap<String, Conversation> conversations;

    private record Conversation(List<Message> messages, Instant lastAccess) {
    }

    public BoundedChatMemoryRepository(int maxConversations, Duration ttl) {
        this(maxConversations, ttl, Clock.systemUTC());
    }

    BoundedChatMemoryRepository(int maxConversations, Duration ttl, Clock clock) {
        if (maxConversations < 1) {
            throw new IllegalArgumentException("maxConversations must be positive");
        }
        this.maxConversations = maxConversations;
        this.ttl = ttl;
        this.clock = clock;
        // accessOrder=true -> thu tu LRU; removeEldestEntry giu kich thuoc toi da.
        this.conversations = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Conversation> eldest) {
                return size() > BoundedChatMemoryRepository.this.maxConversations;
            }
        };
    }

    @Override
    public synchronized List<String> findConversationIds() {
        evictExpired();
        return List.copyOf(conversations.keySet());
    }

    @Override
    public synchronized List<Message> findByConversationId(String conversationId) {
        Conversation entry = conversations.get(conversationId);
        if (entry == null) {
            return List.of();
        }
        if (isExpired(entry)) {
            conversations.remove(conversationId);
            return List.of();
        }
        conversations.put(conversationId, new Conversation(entry.messages(), clock.instant()));
        return List.copyOf(entry.messages());
    }

    @Override
    public synchronized void saveAll(String conversationId, List<Message> messages) {
        evictExpired();
        conversations.put(conversationId, new Conversation(new ArrayList<>(messages), clock.instant()));
    }

    @Override
    public synchronized void deleteByConversationId(String conversationId) {
        conversations.remove(conversationId);
    }

    synchronized int size() {
        return conversations.size();
    }

    private boolean isExpired(Conversation entry) {
        return entry.lastAccess().plus(ttl).isBefore(clock.instant());
    }

    private void evictExpired() {
        conversations.values().removeIf(this::isExpired);
    }
}
