package org.akira.ladux.catalog.controller;

import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Size;
import org.akira.ladux.catalog.dto.response.DiscoverySuggestionResponse;
import org.akira.ladux.catalog.service.DiscoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Smart Search — Intelligent Discovery.
 *
 * <p>{@code GET /api/v1/discovery/suggestions?q=asus}
 * <ul>
 *   <li>q rong: san pham ban chay/noi bat + nhom kham pha nhanh.</li>
 *   <li>q co gia tri: goi y san pham phu hop (khop chinh xac/tien to + gan dung pg_trgm).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    @GetMapping("/suggestions")
    public ResponseEntity<DiscoverySuggestionResponse> suggestions(
            @RequestParam(value = "q", required = false)
            @Size(max = 120, message = "Từ khóa tối đa 120 ký tự") String q) {
        return ResponseEntity.ok(discoveryService.suggest(q));
    }
}
