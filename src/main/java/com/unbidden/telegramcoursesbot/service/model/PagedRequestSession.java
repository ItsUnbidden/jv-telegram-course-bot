package com.unbidden.telegramcoursesbot.service.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PagedRequestSession {
    private final Long id;

    private final LocalDateTime timestamp;

    private int counter;

    public PagedRequestSession(Long id, int counter) {
        this.id = id;
        this.counter = counter;
        this.timestamp = LocalDateTime.now();
    }
}
