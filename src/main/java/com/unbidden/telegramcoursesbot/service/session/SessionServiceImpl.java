package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.repository.SessionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {
    private static final Logger LOGGER = LogManager.getLogger(SessionServiceImpl.class);

    private final SessionRepository sessionRepository;

    @Override
    public void createSession(User user, Consumer<Message> function) {
        removeSessionForUser(user);

        LOGGER.info("Creating new session for user " + user.getId() + "...");
        Session session = new Session();
        session.setUser(user);
        session.setTimestamp(LocalDateTime.now());
        session.setFunction(function);
        sessionRepository.save(session);
        LOGGER.info("Session saved.");
    }

    @Override
    public void removeSessionForUser(User user) {
        LOGGER.info("Removing any current session for user " + user.getId() + "...");
        sessionRepository.remove(user.getId().toString());
        LOGGER.info("Session removed or action skipped.");
    }

    @Override
    public void processResponse(Message message) {
        final User user = message.getFrom();

        LOGGER.info("Response from user " + user.getId() + " recieved. Looking for session...");
        Optional<Session> sessionOpt = sessionRepository.find(user.getId().toString());
        if (sessionOpt.isPresent()) {
            LOGGER.info("Session found.");
            sessionOpt.get().getFunction().accept(message);
            LOGGER.info("Function completed execution. Removing session...");
            sessionRepository.remove(user.getId().toString());
            LOGGER.info("Session removed.");
        }
    }
}
