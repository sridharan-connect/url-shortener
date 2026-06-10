package com.example.demo.urlshortener.service;

import com.example.demo.common.exception.AppException;
import com.example.demo.urlshortener.dto.*;
import com.example.demo.urlshortener.entity.UrlMapping;
import com.example.demo.urlshortener.repository.UrlRepository;
import com.example.demo.urlshortener.util.Base62Util;
import com.example.demo.user.dto.UserDTO;
import com.example.demo.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class UrlService {

    private final UrlRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final AnalyticsQueueService queueService;

    public UrlService(
            UrlRepository repository,
            StringRedisTemplate redisTemplate,
            AnalyticsQueueService queueService
    ) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.queueService = queueService;
    }
    public static final String URL_NOT_FOUND = "URL not found";
    public static final String URL_EXPIRED = "URL expired";

    public String createShortUrl(String originalUrl) {

        // Step 0: Validate (you already added)
        if (!isValidUrl(originalUrl)) {
            throw new AppException("Invalid URL format", HttpStatus.BAD_REQUEST);
        }

        // Step 1: Check duplicate
        Optional<UrlMapping> existing = repository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            UrlMapping mapping = existing.get();
            // CHECK EXPIRY
            if (mapping.getExpiryAt() != null &&
                    mapping.getExpiryAt().isAfter(LocalDateTime.now())) {
                return mapping.getShortCode(); // valid → reuse
            }
        }

        // Step 1: Save without shortCode to get ID
        UrlMapping entity = new UrlMapping();
        entity.setOriginalUrl(originalUrl);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiryAt(LocalDateTime.now().plusHours(24));

        entity = repository.save(entity);

        // Step 2: Generate short code
        String shortCode = Base62Util.encode(entity.getId());

        // Step 3: Update entity
        entity.setShortCode(shortCode);
        repository.save(entity);

        redisTemplate.opsForValue().set(
                "url:" + shortCode,
                originalUrl,
                24,
                TimeUnit.HOURS
        );
        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        // Step 1: Check cache
        String cachedUrl = redisTemplate.opsForValue().get("url:" + shortCode);

        if (cachedUrl != null) {
            System.out.println("Fetching from Redis");
            publishAnalyticsEvent(shortCode);
            return cachedUrl;
        }

        // Step 2: Fetch from DB
        System.out.println("Fetching from DB");
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new AppException(URL_NOT_FOUND, HttpStatus.NOT_FOUND));

        // Step 3:  EXPIRY CHECK
        LocalDateTime expiry = mapping.getExpiryAt();
        if (expiry != null && expiry.isBefore(LocalDateTime.now())) {
            throw new AppException(URL_EXPIRED, HttpStatus.GONE); // 410
        }

        // Step 4: Put in cache
        cacheUrl(mapping);
        publishAnalyticsEvent(shortCode);

        return mapping.getOriginalUrl();
    }

    public UrlStatsResponse getStats(String shortCode) {
        String clicks = redisTemplate.opsForValue().get("url:clicks:" + shortCode);
        String lastAccess = redisTemplate.opsForValue().get("url:lastAccess:" + shortCode);

        UrlStatsResponse response = new UrlStatsResponse();
        response.setShortCode(shortCode);
        response.setClicks(clicks != null ? Long.parseLong(clicks) : 0);
        response.setLastAccess(lastAccess);

        return response;
    }

    public Set<String> getTopUrls() {
        return redisTemplate.opsForZSet().reverseRange("url:top", 0, 4);
    }

    public boolean isValidUrl(String url) {
        try {
            new URI(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void publishAnalyticsEvent(String shortCode) {
        UrlClickEvent event = UrlClickEvent.builder()
                .shortCode(shortCode)
                .timestamp(LocalDateTime.now())
                .build();

        queueService.publish(event);
    }
    public void processAnalytics(String shortCode) {
        redisTemplate.opsForValue().increment("url:clicks:" + shortCode);

        redisTemplate.opsForValue().set(
                "url:lastAccess:" + shortCode,
                LocalDateTime.now().toString()
        );

        redisTemplate.opsForZSet().incrementScore(
                "url:top",
                shortCode,
                1
        );
    }

    private void cacheUrl(UrlMapping mapping) {
        long ttl = Duration.between(
                LocalDateTime.now(),
                mapping.getExpiryAt()
        ).getSeconds();

        redisTemplate.opsForValue().set(
                "url:" + mapping.getShortCode(),
                mapping.getOriginalUrl(),
                ttl,
                TimeUnit.SECONDS
        );
    }

    public void checkRateLimit(String clientIp) {

        String key = "rate_limit:" + clientIp;//If needed we can change URL specific limit, Now implemented Global
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            // first request → set expiry (window)
            redisTemplate.expire(key, 60, TimeUnit.SECONDS); // 1 minute window
        }
        if (count > 10) { // limit = 10 requests per minute
            throw new AppException("Too many requests", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public CursorPageResponse<UrlResponseDto> getUrlsByCursor(Long lastId, int size) {
        PageRequest pageable = PageRequest.of(0, size+1);
        List<UrlMapping> urls = repository.findByIdGreaterThanOrderByIdAsc(lastId, pageable);
        boolean hasNext = urls.size() > size;
        if(hasNext){
            urls.remove(size);
        }
        List<UrlResponseDto> items = urls.stream()
                .map(url -> new UrlResponseDto(
                        url.getId(),
                        url.getOriginalUrl()
                ))
                .toList();
        Long nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).getId();

        return new CursorPageResponse<>(
                items,
                nextCursor,
                hasNext
        );
    }

}
