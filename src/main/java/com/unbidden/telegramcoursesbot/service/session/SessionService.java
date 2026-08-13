package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
public interface SessionService {
    UUID createSession(UserEntity user, Bot bot, Consumer<SessionParamsDto> function);

    void removeSessionsForUserInBot(UserEntity user, Bot bot);

    void processResponse(UserEntity user, Bot bot, Session session, Message message);
}
