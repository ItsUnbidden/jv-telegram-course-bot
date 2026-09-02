package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.model.BotRole;

import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
public interface SessionService<S extends Session> {
    S createSession(BotRole botRole, Consumer<SessionParamsDto> function);

    void removeSessionsForUserInBot(BotRole botRole);

    void processResponse(BotRole botRole, Session session, Message message);
}
