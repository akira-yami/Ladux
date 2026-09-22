package org.akira.ladux.assistant.controller.user;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.assistant.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(
            @RequestParam String query,
            @RequestParam String conversationId
    ) {
        return ResponseEntity.ok(chatbotService.chat(query, conversationId));
    }
}
