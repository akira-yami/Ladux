package org.akira.ladux.ordering.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.ordering.dto.request.CartItemRequest;
import org.akira.ladux.ordering.dto.request.CartQuantityRequest;
import org.akira.ladux.ordering.dto.response.CartResponse;
import org.akira.ladux.ordering.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService service;

    @GetMapping
    public ResponseEntity<CartResponse> getCartByUserId(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getCartByUserId(principal.getId()));
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItemToCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CartItemRequest request) {
        service.addItemToCart(principal.getId(), request.productId(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<Void> updateQuantity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int productId,
            @Valid @RequestBody CartQuantityRequest request) {
        service.updateQuantity(principal.getId(), productId, request.quantity());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItemFromCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int productId) {
        service.removeItemFromCart(principal.getId(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        service.clearCart(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
