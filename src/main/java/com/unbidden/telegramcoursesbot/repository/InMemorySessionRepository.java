package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.service.session.Session;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class InMemorySessionRepository implements SessionRepository, AutoClearable {
    private static final Logger LOGGER = LogManager.getLogger(InMemorySessionRepository.class);

    private static final Map<String, Session> sessions = new HashMap<>();

    @Value("${telegram.bot.message.session.expiration}")
    private Integer expiration;

    @NonNull
    @Override
    public Session save(@NonNull Session session) {
        removeExpired();
        sessions.put(session.getUser().getId().toString(), session);
        return session;
    }

    @NonNull
    @Override
    public Optional<Session> find(@NonNull String userId) {
        removeExpired();
        return Optional.ofNullable(sessions.get(userId));
    }

    @Override
    public void removeExpired() {
        LOGGER.info("Checking for expired sessions...");
        List<String> keysToRemove = new ArrayList<>();

        for (Entry<String, Session> entry : sessions.entrySet()) {
            if (LocalDateTime.now().isAfter(entry.getValue()
                    .getTimestamp().plusSeconds(expiration))) {
                keysToRemove.add(entry.getKey());
            }
        }

        if (keysToRemove.isEmpty()) {
            LOGGER.info("All sessions are valid.");
            return;
        }
        LOGGER.info("Some expired sessions have been found.");
        for (String key : keysToRemove) {
            sessions.remove(key);
        }
    }

    @Override
    public void remove(String userId) {
        sessions.remove(userId);
    }
}
