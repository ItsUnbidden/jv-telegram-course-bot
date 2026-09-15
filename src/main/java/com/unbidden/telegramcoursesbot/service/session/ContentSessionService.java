package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.model.BotRole;
import java.util.UUID;
import java.util.function.Consumer;

public interface ContentSessionService extends SessionService<ContentSession> {
    @Override
    default ContentSession createSession(BotRole botRole, Consumer<SessionParamsDto> function) {
        return createSession(botRole, function, false);
    }

    ContentSession createSession(BotRole botRole, Consumer<SessionParamsDto> function,
            boolean isSkippingConfirmation);
    
    void removeSessionsWithoutConfirmationForUser(BotRole botRole);

    void commit(BotRole botRole, UUID sessionId);

    void resend(BotRole botRole, UUID sessionId);

    void cancel(BotRole botRole, UUID sessionId);
}
