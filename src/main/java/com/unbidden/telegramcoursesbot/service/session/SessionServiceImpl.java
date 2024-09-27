package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.repository.SessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {
    private static final Logger LOGGER = LogManager.getLogger(SessionServiceImpl.class);

    private final SessionRepository sessionRepository;

    @Override
    @NonNull
    public Integer createSession(@NonNull User user, @NonNull Consumer<Message> function,
            boolean isUserOrChatRequestButton) {
        if (isUserOrChatRequestButton) {
            sessionRepository.removeForUserIfNotRequestUserOrChat(user.getId());
        } else {
            removeSessionsForUser(user);
        }

        LOGGER.info("Creating new session for user " + user.getId() + "...");
        Session session = new Session();
        session.setId(ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        session.setUser(user);
        session.setTimestamp(LocalDateTime.now());
        session.setFunction(function);
        session.setUserOrChatRequestButton(isUserOrChatRequestButton);
        sessionRepository.save(session);
        LOGGER.info("Session saved.");
        return session.getId();
    }

    @Override
    public void removeSessionsForUser(@NonNull User user) {
        LOGGER.info("Removing any current sessions for user " + user.getId() + "...");
        sessionRepository.removeForUser(user.getId());
        LOGGER.info("Session removed or action skipped.");
    }

    @Override
    public void processResponse(@NonNull Message message) {
        final User user = message.getFrom();

        LOGGER.info("Response from user " + user.getId() + " recieved. Looking for session...");
        List<Session> sessions = sessionRepository.findForUser(user.getId());
        if (sessions != null && !sessions.isEmpty()) {
            if (sessions.size() == 1) {
                LOGGER.info("Session found.");
                sessions.get(0).getFunction().accept(message);
            } else {
                LOGGER.info(sessions.size() + " button session(s) found.");
                if (message.getChatShared() != null) {
                    sessions.stream()
                            .filter(s -> s.getId().intValue() == Integer.parseInt(
                                message.getChatShared().getRequestId()))
                            .toList().get(0).getFunction().accept(message);
                } else {
                    sessions.stream()
                            .filter(s -> s.getId().intValue() == Integer.parseInt(
                                message.getUserShared().getRequestId()))
                            .toList().get(0).getFunction().accept(message);
                }
            }
            LOGGER.info("Function completed execution.");    
            removeSessionsForUser(user);    
        }
    }
}
