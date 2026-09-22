package org.akira.ladux.notification.controller.user;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.notification.dto.request.ContactRequest;
import org.akira.ladux.notification.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<Map<String, String>> sendContactMessage(
            @Valid @RequestBody ContactRequest request
    ) {
        contactService.sendContactMessage(request);

        return ResponseEntity.ok(Map.of(
                "message",
                "Yêu cầu của bạn đã được gửi thành công"
        ));
    }
}