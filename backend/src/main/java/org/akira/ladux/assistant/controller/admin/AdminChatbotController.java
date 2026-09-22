package org.akira.ladux.assistant.controller.admin;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.assistant.service.ProductEmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/chatbot")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatbotController {
    private final ProductEmbeddingService productEmbeddingService;

    @PostMapping("/index-product/{id}")
    public ResponseEntity<Void> indexProduct(@PathVariable Integer id) {
        productEmbeddingService.indexProduct(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/index-all")
    public ResponseEntity<Integer> indexAllProducts() {
        return ResponseEntity.ok(productEmbeddingService.indexAllProducts());
    }
}
