package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.UUID;
import java.util.function.Consumer;

public interface ContentSessionService extends SessionService {
    @Override
    default UUID createSession(UserEntity user, Bot bot, Consumer<SessionParamsDto> function) {
        return createSession(user, bot, function, false);
    }

    UUID createSession(UserEntity user, Bot bot, Consumer<SessionParamsDto> function,
            boolean isSkippingConfirmation);
    
    void removeSessionsWithoutConfirmationForUser(UserEntity user, Bot bot);

    void commit(UserEntity user, Bot bot, UUID sessionId);

    void resend(UserEntity user, Bot bot, UUID sessionId);

    void cancel(UserEntity user, Bot bot, UUID sessionId);
}
