package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.SessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class UserOrChatRequestSessionService implements SessionService<UserOrChatRequestSession> {
    private static final Logger LOGGER = LogManager
            .getLogger(UserOrChatRequestSessionService.class);

    private final SessionRepository sessionRepository;

    @Override
    public UserOrChatRequestSession createSession(UserEntity user, Bot bot, Consumer<SessionParamsDto> function) {
        sessionRepository.removeContentSessionsForUserInBot(user.getId(), bot);

        LOGGER.debug("Creating new user or chat request session for user "
                + user.getId() + "...");
        final UserOrChatRequestSession session = new UserOrChatRequestSession();

        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setBot(bot);
        session.setTimestamp(LocalDateTime.now());
        session.setFunction(function);
        session.setRequestId(ThreadLocalRandom.current().nextInt());
        sessionRepository.save(session);
        LOGGER.debug("Session saved.");
        
        return session;
    }

    @Override
    public void removeSessionsForUserInBot(UserEntity user, Bot bot) {
        sessionRepository.removeForUserInBot(user.getId(), bot);
    }

    @Override
    public void processResponse(UserEntity user, Bot bot, Session session, Message message) {
        removeSessionsForUserInBot(user, bot);
        session.getFunction().accept(new SessionParamsDto(user, bot, List.of(message)));
    }
}
