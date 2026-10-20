package org.akira.ladux.assistant.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.assistant.dto.request.ContextualChatRequest;
import org.akira.ladux.assistant.dto.response.ContextualChatResponse;
import org.akira.ladux.assistant.service.ChatbotService;
import org.akira.ladux.assistant.service.ProductEmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/chatbot", "/api/v1/admin/chatbot"})
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ProductEmbeddingService productEmbeddingService;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(
            @RequestParam @NotBlank @Size(max = 2000) String query,
            @RequestParam @NotBlank @Size(max = 100) String conversationId
    ) {
        return ResponseEntity.ok(chatbotService.chat(query, conversationId));
    }

    @PostMapping("/contextual-chat")
    public ResponseEntity<ContextualChatResponse> contextualChat(@Valid @RequestBody ContextualChatRequest request) {
        return ResponseEntity.ok(chatbotService.contextualChat(request));
    }

    @PostMapping("/index-product/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> indexProduct(@PathVariable Integer id) {
        productEmbeddingService.indexProduct(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/index-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> indexAllProducts() {
        return ResponseEntity.ok(productEmbeddingService.indexAllProducts());
    }
}
