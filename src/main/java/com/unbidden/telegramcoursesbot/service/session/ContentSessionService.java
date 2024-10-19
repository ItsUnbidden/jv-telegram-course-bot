package com.unbidden.telegramcoursesbot.service.session;

import org.springframework.lang.NonNull;

public interface ContentSessionService extends SessionService {
    void commit(@NonNull Integer sessionId);

    void resend(@NonNull Integer sessionId);

    void cancel(@NonNull Integer sessionId);
}
