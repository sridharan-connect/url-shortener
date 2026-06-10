package com.example.demo.urlshortener.service;

import com.example.demo.urlshortener.dto.UrlClickEvent;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class AnalyticsQueueService {
    private final BlockingQueue<UrlClickEvent> queue
            = new LinkedBlockingQueue<>();
    private final BlockingQueue<UrlClickEvent> deadLetterQueue
            = new LinkedBlockingQueue<>();

    public boolean publish(UrlClickEvent event){
        return queue.offer(event);
    }

    public UrlClickEvent take() throws InterruptedException {
        return queue.take();
    }

    public void publishToDeadLetterQueue(UrlClickEvent event) {
        deadLetterQueue.offer(event);
    }

}
