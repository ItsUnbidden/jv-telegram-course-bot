package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.model.BotRole;
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
    public UserOrChatRequestSession createSession(BotRole botRole, Consumer<SessionParamsDto> function) {
        sessionRepository.removeContentSessionsForUserInBot(botRole.getUser().getId(), botRole.getBot());

        LOGGER.debug("Creating new user or chat request session for user " + botRole.getUser().getId() + "...");
        final UserOrChatRequestSession session = new UserOrChatRequestSession();

        session.setId(UUID.randomUUID());
        session.setBotRole(botRole);
        session.setTimestamp(LocalDateTime.now());
        session.setFunction(function);
        session.setRequestId(ThreadLocalRandom.current().nextInt());
        sessionRepository.save(session);
        LOGGER.debug("Session saved.");
        
        return session;
    }

    @Override
    public void removeSessionsForUserInBot(BotRole botRole) {
        sessionRepository.removeForUserInBot(botRole.getUser().getId(), botRole.getBot());
    }

    @Override
    public void processResponse(BotRole botRole, Session session, Message message) {
        removeSessionsForUserInBot(botRole);
        session.getFunction().accept(new SessionParamsDto(botRole, List.of(message)));
    }
}
