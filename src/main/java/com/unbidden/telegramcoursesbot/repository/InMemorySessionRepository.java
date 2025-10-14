package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
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
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InMemorySessionRepository implements SessionRepository, AutoClearable {
    private static final Logger LOGGER = LogManager.getLogger(InMemorySessionRepository.class);

    private static final int INITIAL_EXPIRY_CHECK_DELAY = 10000;

    private static final ConcurrentMap<Integer, Session> sessions = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Long, List<Session>> sessionsIndexedByUser =
            new ConcurrentHashMap<>();

    private static final String CONFIRM_MENU_TERMINATOR = "session_%s_terminator";

    private static final String MENU_COMMIT_CONTENT_EXPIRED_TERMINAL_PAGE =
            "menu_commit_content_expired_terminal_page";

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    @Value("${telegram.bot.message.session.expiration}")
    private Integer expiration;

    @NonNull
    @Override
    public Session save(@NonNull Session session) {
        sessions.put(session.getId(), session);
        indexByUser(session.getUser(), session);
        return session;
    }

    @NonNull
    @Override
    public Optional<Session> find(@NonNull Integer id) {
        return Optional.ofNullable(sessions.get(id));
    }

    @Override
    @Scheduled(initialDelay = INITIAL_EXPIRY_CHECK_DELAY,
            fixedDelayString = "${telegram.bot.message.session.schedule.delay}")
    public void removeExpired() {
        LOGGER.trace("Checking for expired sessions...");
        List<Integer> keysToRemove = new ArrayList<>();

        for (Entry<Integer, Session> entry : sessions.entrySet()) {
            if (LocalDateTime.now().isAfter(entry.getValue()
                    .getTimestamp().plusSeconds(expiration))) {
                if (entry.getValue() instanceof ContentSession) {
                    try {
                        menuService.terminateMenuGroup(entry.getValue().getUser(),
                                entry.getValue().getBot(), CONFIRM_MENU_TERMINATOR
                                .formatted(entry.getValue().getId()), localizationLoader
                                .getLocalizationForUser(MENU_COMMIT_CONTENT_EXPIRED_TERMINAL_PAGE,
                                entry.getValue().getUser()));
                        LOGGER.debug("An MTG for session " + entry.getValue().getId()
                                + " was terminated after the session expired.");
                    } catch (EntityNotFoundException e) {
                        LOGGER.debug("There is no MTG for session "
                                + entry.getValue().getId() + ".");
                    }
                }
                keysToRemove.add(entry.getKey());
            }
        }

        if (keysToRemove.isEmpty()) {
            LOGGER.trace("All sessions are valid.");
            return;
        }
        LOGGER.trace("Some expired sessions have been found.");
        for (Integer key : keysToRemove) {
            removeFromIndexedSessions(sessions.get(key));
            sessions.remove(key);
        }
    }

    @Override
    public void removeForUserInBot(@NonNull Long userId, @NonNull Bot bot) {
        final List<Session> userSessions = sessionsIndexedByUser.get(userId);
        if (userSessions != null) {
            final List<Integer> keysToRemove = userSessions.stream()
                    .filter(s -> s.getBot().equals(bot)).map(s -> s.getId()).toList();
            for (Integer key : keysToRemove) {
                removeFromIndexedSessions(sessions.remove(key));
            }
        }
    }

    @Override
    public void removeContentSessionsForUserInBot(@NonNull Long userId, @NonNull Bot bot) {
        final List<Session> userSessions = sessionsIndexedByUser.get(userId);

        if (userSessions != null) {
            final List<Integer> keysToRemove = userSessions.stream()
                    .filter(s -> s.getBot().equals(bot)
                        && s.getClass().equals(ContentSession.class))
                    .map(s -> s.getId()).toList();
            for (Integer key : keysToRemove) {
                removeFromIndexedSessions(sessions.remove(key));
            }
        }
    }

    @Override
    public void removeSessionsWithoutConfirmationForUserInBot(@NonNull Long userId,
            @NonNull Bot bot) {
        final List<Session> userSessions = sessionsIndexedByUser.get(userId);

        if (userSessions != null) {
            final List<Integer> keysToRemove = userSessions.stream()
                    .filter(s -> s.getBot().equals(bot)
                        && s.getClass().equals(ContentSession.class)
                        && ((ContentSession)s).isSkippingConfirmation())
                    .map(s -> s.getId()).toList();
            for (Integer key : keysToRemove) {
                removeFromIndexedSessions(sessions.remove(key));
            }
        }
    }

    @Override
    public void removeUserOrChatRequestSessionsForUserInBot(@NonNull Long userId,
            @NonNull Bot bot) {
        final List<Session> userSessions = sessionsIndexedByUser.get(userId);

        if (userSessions != null) {
            final List<Integer> keysToRemove = userSessions.stream()
                    .filter(s -> s.getBot().equals(bot)
                        && s.getClass().equals(UserOrChatRequestSession.class))
                    .map(s -> s.getId()).toList();
            for (Integer key : keysToRemove) {
                removeFromIndexedSessions(sessions.remove(key));
            }
        }
    }

    @Override
    @NonNull
    public List<Session> findForUserInBot(@NonNull Long userId, @NonNull Bot bot) {
        final List<Session> sessions = sessionsIndexedByUser.get(userId);
        return (sessions != null) ? sessions.stream()
                .filter(s -> s.getBot().equals(bot)).toList() : new ArrayList<>();
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
