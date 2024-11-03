package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.session.ContentSession;
import com.unbidden.telegramcoursesbot.service.session.Session;
import com.unbidden.telegramcoursesbot.service.session.UserOrChatRequestSession;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

@Repository
public class InMemorySessionRepository implements SessionRepository, AutoClearable {
    private static final Logger LOGGER = LogManager.getLogger(InMemorySessionRepository.class);

    private static final int INITIAL_EXPIRY_CHECK_DELAY = 10000;

    private static final ConcurrentMap<Integer, Session> sessions = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Long, List<Session>> sessionsIndexedByUser =
            new ConcurrentHashMap<>();

    @Value("${telegram.bot.message.session.expiration}")
    private Integer expiration;

    @NonNull
    @Override
    public Session save(@NonNull Session session) {
        sessions.put(session.getId(), session);
        indexByUser(session.getUser(), session);
        return session;
    }

    // TODO: fix the issue where expired sessions might be considered legit
    @NonNull
    @Override
    public Optional<Session> find(@NonNull Integer id) {
        return Optional.ofNullable(sessions.get(id));
    }

    @Override
    @Scheduled(initialDelay = INITIAL_EXPIRY_CHECK_DELAY,
            fixedDelayString = "${telegram.bot.message.session.schedule.delay}")
    public void removeExpired() {
        LOGGER.debug("Checking for expired sessions...");
        List<Integer> keysToRemove = new ArrayList<>();

        for (Entry<Integer, Session> entry : sessions.entrySet()) {
            if (LocalDateTime.now().isAfter(entry.getValue()
                    .getTimestamp().plusSeconds(expiration))) {
                keysToRemove.add(entry.getKey());
            }
        }

        if (keysToRemove.isEmpty()) {
            LOGGER.debug("All sessions are valid.");
            return;
        }
        LOGGER.debug("Some expired sessions have been found.");
        for (Integer key : keysToRemove) {
            removeFromIndexedSessions(sessions.get(key));
            sessions.remove(key);
        }
    }

    @Override
    public void removeForUser(@NonNull Long userId) {
        final List<Session> userSessions = sessionsIndexedByUser.get(userId);

        if (userSessions != null) {
            final List<Integer> keysToRemove = userSessions.stream()
                    .map(s -> s.getId()).toList();
            for (Integer key : keysToRemove) {
                removeFromIndexedSessions(sessions.remove(key));
            }
        }
    }

    @Override
    public void removeContentSessionsForUser(@NonNull Long userId) {
        final List<Session> userSessions = sessionsIndexedByUser.get(userId);

        if (userSessions != null) {
            final List<Integer> keysToRemove = userSessions.stream()
                    .filter(s -> s.getClass().equals(ContentSession.class))
                    .map(s -> s.getId()).toList();
            for (Integer key : keysToRemove) {
                removeFromIndexedSessions(sessions.remove(key));
            }
        }
    }

    @Override
    public void removeUserOrChatRequestSessionsForUser(@NonNull Long userId) {
        final List<Session> userSessions = sessionsIndexedByUser.get(userId);

        if (userSessions != null) {
            final List<Integer> keysToRemove = userSessions.stream()
                    .filter(s -> s.getClass().equals(UserOrChatRequestSession.class))
                    .map(s -> s.getId()).toList();
            for (Integer key : keysToRemove) {
                removeFromIndexedSessions(sessions.remove(key));
            }
        }
    }

    @Override
    @NonNull
    public List<Session> findForUser(@NonNull Long userId) {
        final List<Session> sessions = sessionsIndexedByUser.get(userId);
        return (sessions != null) ? sessions : new ArrayList<>();
    }

    private void indexByUser(UserEntity user, Session session) {
        List<Session> sessions = sessionsIndexedByUser.get(user.getId());

        if (sessions != null) {
            LOGGER.debug("For user " + user.getId()
                    + " there is a list for sessions in index map.");
            sessions.add(session);
            return;
        }
        LOGGER.debug("For user " + user.getId()
                + " there is no list for sessions in index map.");
        sessions = new ArrayList<>();
        sessions.add(session);
        sessionsIndexedByUser.put(user.getId(), sessions);
    }

    private void removeFromIndexedSessions(Session session) {
        sessionsIndexedByUser.get(session.getUser().getId()).remove(session);
        LOGGER.debug("Session for user " + session.getUser().getId()
                + " was removed from the index map.");

        final List<Long> usersWithNoSessions = new ArrayList<>();

        sessionsIndexedByUser.forEach((id, s) -> {
            if (s.isEmpty()) {
                usersWithNoSessions.add(id);
            }
        });
        if (!usersWithNoSessions.isEmpty()) {
            LOGGER.debug("Users " + usersWithNoSessions
                    + " have no sessions. Removing them from the index map...");
        }
        for (Long id : usersWithNoSessions) {
            sessionsIndexedByUser.remove(id);
        }
    }
}
