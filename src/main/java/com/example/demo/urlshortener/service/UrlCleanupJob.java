package com.example.demo.urlshortener.service;

import com.example.demo.urlshortener.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UrlCleanupJob {

    @Autowired
    private UrlRepository repository;

    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000)
    public void deleteExpiredUrls() {
        int deleted = repository.deleteByExpiryAtBefore(LocalDateTime.now());
    }
}
