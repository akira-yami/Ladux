package org.akira.ladux.customer.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.dto.request.UserAddressRequest;
import org.akira.ladux.customer.dto.response.UserAddressResponse;
import org.akira.ladux.customer.service.UserAddressService;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService service;

    @GetMapping("/api/v1/user-addresses/{addressId}")
    public ResponseEntity<UserAddressResponse> getUserAddressById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int addressId) {
        return ResponseEntity.ok(service.getUserAddressById(principal.getId(), addressId));
    }

    @GetMapping("/api/v1/user-addresses/user")
    public ResponseEntity<List<UserAddressResponse>> getUserAddressesByUserId(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getUserAddressesByUserId(principal.getId()));
    }

    @GetMapping("/api/v1/user-addresses/default")
    public ResponseEntity<List<UserAddressResponse>> getDefaultUserAddressesByUserId(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getDefaultUserAddressesByUserId(principal.getId()));
    }

    @PostMapping("/api/v1/user-addresses")
    public ResponseEntity<UserAddressResponse> createUserAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createUserAddress(principal.getId(), request));
    }

    @PutMapping("/api/v1/user-addresses/{addressId}")
    public ResponseEntity<UserAddressResponse> updateUserAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int addressId,
            @Valid @RequestBody UserAddressRequest request) {
        return ResponseEntity.ok(service.updateUserAddress(principal.getId(), addressId, request));
    }

    @DeleteMapping("/api/v1/user-addresses/{addressId}")
    public ResponseEntity<Void> deleteUserAddressById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int addressId) {
        service.deleteUserAddressById(principal.getId(), addressId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/admin/user-addresses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserAddressResponse>> getAllUserAddresses(Pageable pageable) {
        return ResponseEntity.ok(service.getAllUserAddresses(pageable));
    }

    @GetMapping("/api/v1/admin/user-addresses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAddressResponse> getUserAddressByIdForAdmin(@PathVariable int id) {
        return ResponseEntity.ok(service.getUserAddressByIdForAdmin(id));
    }
}
