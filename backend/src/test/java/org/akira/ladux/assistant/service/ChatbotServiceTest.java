package org.akira.ladux.assistant.service;

import com.google.genai.errors.ClientException;
import java.math.BigDecimal;
import java.util.List;
import org.akira.ladux.assistant.dto.request.ContextualChatRequest;
import org.akira.ladux.assistant.dto.response.ContextualChatResponse.ProductCard;
import org.akira.ladux.assistant.service.result.ChatbotContext;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatbotServiceTest {
    @Test
    void googleSdkEmbeddingClientFailureUsesFallbackForContextualAndLegacyEndpoints() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        ChatbotCatalogService catalog = mock(ChatbotCatalogService.class);
        when(builder.build()).thenReturn(client);
        when(catalog.resolve("Tư vấn", List.of()))
                .thenThrow(new ClientException(400, "INVALID_ARGUMENT", "API key not valid"));
        var service = new ChatbotService(builder, catalog);

        var response = service.contextualChat(new ContextualChatRequest("Tư vấn", "id", List.of()));

        assertTrue(response.products().isEmpty());
        assertTrue(response.text().contains("tạm thời không khả dụng"));
        assertTrue(service.chat("Tư vấn", "legacy").contains("tạm thời không khả dụng"));
        verifyNoInteractions(client);
    }

    @Test
    void unavailableEmbeddingProviderDegradesGracefullyButDatabaseFailuresAreNotHidden() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        ChatbotCatalogService catalog = mock(ChatbotCatalogService.class);
        when(builder.build()).thenReturn(client);
        when(catalog.resolve("Tư vấn", List.of())).thenThrow(new NonTransientAiException("provider unavailable"));
        var service = new ChatbotService(builder, catalog);

        var result = service.contextualChat(new ContextualChatRequest("Tư vấn", "id", List.of()));
        assertTrue(result.products().isEmpty());
        assertTrue(result.text().contains("tạm thời không khả dụng"));
        verifyNoInteractions(client);

        doThrow(new DataAccessResourceFailureException("database unavailable")).when(catalog).resolve("Tư vấn", List.of());
        assertThrows(DataAccessResourceFailureException.class,
                () -> service.contextualChat(new ContextualChatRequest("Tư vấn", "id", List.of())));
    }

    @Test
    void structuredCardsComeOnlyFromVerifiedCatalogAndContextualCallsHaveNoSharedMemory() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec prompt = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        ChatbotCatalogService catalog = mock(ChatbotCatalogService.class);
        when(builder.build()).thenReturn(client);
        when(client.prompt()).thenReturn(prompt);
        when(prompt.call()).thenReturn(call);
        when(call.content()).thenReturn("Diễn giải từ mô hình");
        ProductCard card = new ProductCard(1, 101, "Laptop", BigDecimal.valueOf(18_000_000), null);
        when(catalog.resolve("Tư vấn", List.of(101))).thenReturn(new ChatbotContext("Dữ liệu hiện tại", List.of(card)));

        var result = new ChatbotService(builder, catalog).contextualChat(new ContextualChatRequest(" Tư vấn ", "same-id", List.of(101)));

        assertEquals(List.of(card), result.products());
        assertEquals("Diễn giải từ mô hình", result.text());
        verify(prompt).system(contains("Dữ liệu hiện tại"));
        verify(prompt).user("Tư vấn");
        verify(prompt).call();
        verifyNoMoreInteractions(prompt);
    }

    @Test
    void unavailableSkuContextReturnsNoCardsAndDoesNotCallModel() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        ChatbotCatalogService catalog = mock(ChatbotCatalogService.class);
        when(builder.build()).thenReturn(client);
        when(catalog.resolve("Tư vấn", List.of(101))).thenReturn(new ChatbotContext("", List.of()));

        var result = new ChatbotService(builder, catalog).contextualChat(new ContextualChatRequest("Tư vấn", "id", List.of(101)));

        assertTrue(result.products().isEmpty());
        assertTrue(result.text().contains("chưa có cấu hình"));
        verifyNoInteractions(client);
    }

    @Test
    void modelFailurePreservesVerifiedCardsAndLegacyEndpointStillReturnsString() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        ChatbotCatalogService catalog = mock(ChatbotCatalogService.class);
        when(builder.build()).thenReturn(client);
        when(client.prompt()).thenThrow(new IllegalStateException("model unavailable"));
        ProductCard card = new ProductCard(1, 101, "Laptop", BigDecimal.TEN, null);
        when(catalog.resolve(eq("Tư vấn"), any())).thenReturn(new ChatbotContext("Public catalog", List.of(card)));
        var service = new ChatbotService(builder, catalog);

        assertEquals(List.of(card), service.contextualChat(new ContextualChatRequest("Tư vấn", "id", null)).products());
        assertTrue(service.chat("Tư vấn", "legacy").contains("tạm thời không khả dụng"));
        assertThrows(BusinessRuleException.class, () -> service.chat("a".repeat(2001), "id"));
    }
}
