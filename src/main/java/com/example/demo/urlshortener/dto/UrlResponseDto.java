package com.example.demo.urlshortener.dto;

public class UrlResponseDto {
    private Long id;
    private String originalUrl;

    public UrlResponseDto(Long id, String originalUrl) {
        this.id = id;
        this.originalUrl = originalUrl;
    }

    public Long getId() {
        return id;
    }

    public String getOriginal_url() {
        return originalUrl;
    }
}
