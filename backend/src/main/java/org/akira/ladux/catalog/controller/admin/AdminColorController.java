package org.akira.ladux.catalog.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.ColorRequest;
import org.akira.ladux.catalog.dto.response.ColorResponse;
import org.akira.ladux.catalog.entity.Color;
import org.akira.ladux.catalog.mapper.ColorMapper;
import org.akira.ladux.catalog.service.ColorService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/color")
@RequiredArgsConstructor
public class AdminColorController {
    private final ColorService colorService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ColorResponse>> getAllColors(Pageable pageable) {
        return ResponseEntity.ok(colorService.getAllColors(pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ColorResponse> addColor(@Valid @RequestBody ColorRequest request) {
         Color color = colorService.addColor(request);
         return new ResponseEntity<>(ColorMapper.fromEntity(color), HttpStatus.CREATED);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ColorResponse> updateColor(@PathVariable int id, @Valid @RequestBody ColorRequest request) {
        Color updatedColor = colorService.updateColor(id, request);
        return new ResponseEntity<>(ColorMapper.fromEntity(updatedColor), HttpStatus.OK);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteColor(@PathVariable int id) {
        colorService.deleteColor(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
