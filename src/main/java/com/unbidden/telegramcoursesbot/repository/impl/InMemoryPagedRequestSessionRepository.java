package com.unbidden.telegramcoursesbot.repository.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.unbidden.telegramcoursesbot.repository.AutoClearable;
import com.unbidden.telegramcoursesbot.repository.CustomGeneralRepository;
import com.unbidden.telegramcoursesbot.service.model.PagedRequestSession;

public abstract class InMemoryPagedRequestSessionRepository<T extends PagedRequestSession>
        implements CustomGeneralRepository<Long, T>, AutoClearable {
    private final ConcurrentMap<Long, T> STORAGE = new ConcurrentHashMap<>(); 

    private final Integer expiration;

    public InMemoryPagedRequestSessionRepository(Integer expiration) {
        this.expiration = expiration;
    }

    @Override
    public T save(T type) {
        return STORAGE.put(type.getId(), type);
    }

    @Override
    public Optional<T> find(Long id) {
        return Optional.ofNullable(STORAGE.get(id));
    }

    @Override
    public void removeExpired() {
        final List<Long> keysToRemove = new ArrayList<>();

        for (final Entry<Long, T> entry : STORAGE.entrySet()) {
            if (LocalDateTime.now().isAfter(entry.getValue().getTimestamp().plusSeconds(expiration))) {
                keysToRemove.add(entry.getKey());
            }
        }

        for (final Long key : keysToRemove) {
            STORAGE.remove(key);
        }
    }

    public void remove(Long botRoleId) {
        STORAGE.remove(botRoleId);
    }
}
