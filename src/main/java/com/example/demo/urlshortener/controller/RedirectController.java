package com.example.demo.urlshortener.controller;

import com.example.demo.urlshortener.dto.UrlStatsResponse;
import com.example.demo.urlshortener.service.RedisTestService;
import com.example.demo.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
public class RedirectController {

    @Autowired
    private final UrlService service;
    private final RedisTestService redisTestService;

    public RedirectController(UrlService service,
                              RedisTestService redisTestService) {
        this.service = service;
        this.redisTestService = redisTestService;
    }

    @GetMapping("/test-redis")
    public String testRedis() {
       return redisTestService.test();
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        service.checkRateLimit(clientIp);
        String originalUrl = service.getOriginalUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    @GetMapping("/api/v1/urls/{shortCode}/stats")
    public UrlStatsResponse getStats(@PathVariable String shortCode) {
        return service.getStats(shortCode);
    }

    @GetMapping("/api/v1/urls/top")
    public Set<String> getTopUrls() {
        return service.getTopUrls();
    }
}
