package com.example.demo.urlshortener.controller;

import com.example.demo.urlshortener.dto.CursorPageResponse;
import com.example.demo.urlshortener.dto.UrlRequestDto;
import com.example.demo.urlshortener.dto.UrlResponseDto;
import com.example.demo.urlshortener.service.UrlService;
import com.example.demo.user.dto.UserDTO;
import com.example.demo.user.model.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
public class CreateAndGetController {

    @Autowired
    private UrlService service;

    @PostMapping
    public ResponseEntity<String> create(@Valid @RequestBody UrlRequestDto request) {
        String shortCode = service.createShortUrl(request.getUrl());
        return ResponseEntity.ok("http://localhost:8080/" + shortCode);
    }

    @GetMapping("/cursor")
    public ResponseEntity<ApiResponse<CursorPageResponse<UrlResponseDto>>> getUrlsByCursor(
            @RequestParam(defaultValue = "0") Long lastId,
            @RequestParam(defaultValue = "5") int size
    ) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "URLs fetched successfully",
                        service.getUrlsByCursor(lastId, size)
                )
        );
    }
}
