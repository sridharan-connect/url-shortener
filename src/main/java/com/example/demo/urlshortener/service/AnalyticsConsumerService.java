package com.example.demo.urlshortener.service;

import com.example.demo.urlshortener.dto.UrlClickEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsConsumerService {

    private final AnalyticsQueueService queueService;
    private final UrlService urlService;
    // Thread pool with 2 worker threads
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(
                    2,
                    runnable -> {
                        Thread thread = new Thread(runnable);
                        thread.setName("analytics-consumer-thread");
                        return thread;
                    }
            );

    @PostConstruct
    public void startConsumer() {

        executorService.submit(() ->  {

            while (true) {

                try {

                    UrlClickEvent event = queueService.take();

                    processWithRetry(event);

                } catch (Exception e) {
                    log.error("Error processing analytics event", e);
                }
            }

        });
    }

    private void processEvent(UrlClickEvent event) {

        log.info("Processing analytics event for shortCode={}",
                event.getShortCode());
         urlService.processAnalytics(event.getShortCode());
    }

    private void processWithRetry(UrlClickEvent event) {

        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {

            try {

                processEvent(event);

                return;

            } catch (Exception e) {

                attempt++;

                log.error(
                        "Retry attempt {} failed for shortCode={}",
                        attempt,
                        event.getShortCode(),
                        e
                );

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.error(
                "Moving event to DLQ for shortCode={}",
                event.getShortCode()
        );

        queueService.publishToDeadLetterQueue(event);
    }
}