package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.function.TriConsumer;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
public interface SessionService {
    UUID createSession(UserEntity user, Bot bot, TriConsumer<UserEntity, Bot, List<Message>> function);

    void removeSessionsForUserInBot(UserEntity user, Bot bot);

    void processResponse(UserEntity user, Bot bot, Session session, Message message);
}
