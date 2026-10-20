package org.akira.ladux.assistant.controller;

import java.util.List;
import org.akira.ladux.assistant.dto.response.ContextualChatResponse;
import org.akira.ladux.assistant.service.ChatbotService;
import org.akira.ladux.assistant.service.ProductEmbeddingService;
import org.akira.ladux.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatbotControllerTest {
    @Test
    void acceptsContextualJsonAndKeepsLegacyStringEndpoint() throws Exception {
        ChatbotService service = mock(ChatbotService.class);
        when(service.contextualChat(any())).thenReturn(new ContextualChatResponse("Tư vấn", List.of()));
        when(service.chat("test", "legacy")).thenReturn("Legacy response");
        var mvc = MockMvcBuilders.standaloneSetup(new ChatbotController(service, mock(ProductEmbeddingService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/v1/chatbot/contextual-chat").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"Tư vấn","conversationId":"browser-1","variantIds":[101]}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.text").value("Tư vấn"))
                .andExpect(jsonPath("$.products").isArray());
        mvc.perform(post("/api/v1/admin/chatbot/chat").param("query", "test").param("conversationId", "legacy"))
                .andExpect(status().isOk()).andExpect(content().string("Legacy response"));
    }

    @Test
    void rejectsOversizedQueryConversationAndInvalidSkuListBeforeService() throws Exception {
        ChatbotService service = mock(ChatbotService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ChatbotController(service, mock(ProductEmbeddingService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (String body : List.of(
                "{\"query\":\" \" ,\"conversationId\":\"id\"}",
                "{\"query\":\"" + "a".repeat(2001) + "\",\"conversationId\":\"id\"}",
                "{\"query\":\"laptop\",\"conversationId\":\"" + "a".repeat(101) + "\"}",
                "{\"query\":\"laptop\",\"conversationId\":\"id\",\"variantIds\":[1,2,3,4,5,6]}",
                "{\"query\":\"laptop\",\"conversationId\":\"id\",\"variantIds\":[0]}",
                "{\"query\":\"laptop\",\"conversationId\":\"id\",\"variantIds\":[null]}")) {
            mvc.perform(post("/api/v1/chatbot/contextual-chat").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
