package com.unbidden.telegramcoursesbot.service.session;

import java.util.function.Consumer;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

public interface SessionService {
    void createSession(User user, Consumer<Message> function);

    void removeSessionForUser(User user);

    void processResponse(Message message);
}
