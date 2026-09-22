package org.akira.ladux.ordering.controller.user;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.ordering.dto.response.OrderHistoryResponse;
import org.akira.ladux.ordering.service.OrderHistoryService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/order-histories")
@RequiredArgsConstructor
public class OrderHistoryController {
    private final OrderHistoryService service;

    @GetMapping("/my")
    public ResponseEntity<PageResponse<OrderHistoryResponse>> getOrdersHistoryByUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.getOrdersHistoryByUser(userPrincipal.getId(), pageable));
    }
}
