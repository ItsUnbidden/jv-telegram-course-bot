package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.function.TriConsumer;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface ContentSessionService extends SessionService {
    UUID createSession(UserEntity user, Bot bot, TriConsumer<UserEntity, Bot, List<Message>> function,
            boolean isSkippingConfirmation);
    
    void removeSessionsWithoutConfirmationForUser(UserEntity user, Bot bot);

    void commit(UserEntity user, Bot bot, UUID sessionId);

    void resend(UserEntity user, Bot bot, UUID sessionId);

    void cancel(UserEntity user, Bot bot, UUID sessionId);
}
