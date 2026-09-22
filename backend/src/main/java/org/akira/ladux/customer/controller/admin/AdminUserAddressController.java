package org.akira.ladux.customer.controller.admin;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.dto.response.UserAddressResponse;
import org.akira.ladux.customer.service.UserAddressService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/user-addresses")
@RequiredArgsConstructor
public class AdminUserAddressController {
    private final UserAddressService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserAddressResponse>> getAllUserAddresses(Pageable pageable) {
        return ResponseEntity.ok(service.getAllUserAddresses(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAddressResponse> getUserAddressById(@PathVariable int id) {
        return ResponseEntity.ok(service.getUserAddressByIdForAdmin(id));
    }
}
