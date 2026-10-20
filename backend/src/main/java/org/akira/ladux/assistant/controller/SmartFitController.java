package org.akira.ladux.assistant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.assistant.dto.request.SmartFitRequest;
import org.akira.ladux.assistant.dto.response.SmartFitResponse;
import org.akira.ladux.assistant.service.SmartFitService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/smartfit")
@RequiredArgsConstructor
public class SmartFitController {
    private final SmartFitService smartFitService;

    @PostMapping("/recommend")
    public SmartFitResponse recommend(@Valid @RequestBody SmartFitRequest request) {
        return smartFitService.recommend(request);
    }
}
