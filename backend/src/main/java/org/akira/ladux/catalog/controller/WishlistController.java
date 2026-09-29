package org.akira.ladux.catalog.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.WishlistRequest;
import org.akira.ladux.catalog.dto.response.WishlistResponse;
import org.akira.ladux.catalog.service.WishlistService;
import org.akira.ladux.identity.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService service;

    @PostMapping
    public ResponseEntity<Void> addItemToWishlist(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody WishlistRequest request) {
        service.addItemToWishlist(principal.getId(), request.productId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getWishlistsByUserId(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getWishlistsByUserId(principal.getId()));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeItemFromWishlist(@AuthenticationPrincipal UserPrincipal principal, @PathVariable int productId) {
        service.removeItemFromWishlist(principal.getId(), productId);
        return ResponseEntity.noContent().build();
    }
}
