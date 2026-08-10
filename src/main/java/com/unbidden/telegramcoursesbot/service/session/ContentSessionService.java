package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface ContentSessionService extends SessionService {
    UUID createSession(UserEntity user, Bot bot, Consumer<List<Message>> function, boolean isSkippingConfirmation);
    
    void removeSessionsWithoutConfirmationForUser(UserEntity user, Bot bot);

    void commit(UUID sessionId, UserEntity user);

    void resend(UUID sessionId, UserEntity user);

    void cancel(UUID sessionId, UserEntity user);
}
