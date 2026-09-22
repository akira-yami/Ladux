package org.akira.ladux.customer.controller.user;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.dto.request.UserAddressRequest;
import org.akira.ladux.customer.dto.response.UserAddressResponse;
import org.akira.ladux.customer.service.UserAddressService;
import org.akira.ladux.identity.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user-addresses")
@RequiredArgsConstructor
public class UserAddressController {
    private final UserAddressService service;

    @GetMapping("/{addressId}")
    public ResponseEntity<UserAddressResponse> getUserAddressById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int addressId
    ) {
        return ResponseEntity.ok(service.getUserAddressById(principal.getId(), addressId));
    }

    @GetMapping("/user")
    public ResponseEntity<List<UserAddressResponse>> getUserAddressesByUserId(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getUserAddressesByUserId(principal.getId()));
    }

    @GetMapping("/default")
    public ResponseEntity<List<UserAddressResponse>> getDefaultUserAddressesByUserId(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getDefaultUserAddressesByUserId(principal.getId()));
    }

    @PostMapping
    public ResponseEntity<UserAddressResponse> createUserAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserAddressRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createUserAddress(principal.getId(), request));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<UserAddressResponse> updateUserAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int addressId,
            @Valid @RequestBody UserAddressRequest request
    ) {
        return ResponseEntity.ok(service.updateUserAddress(principal.getId(), addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteUserAddressById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int addressId
    ) {
        service.deleteUserAddressById(principal.getId(), addressId);
        return ResponseEntity.noContent().build();
    }
}