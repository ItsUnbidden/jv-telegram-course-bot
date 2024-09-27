package com.unbidden.telegramcoursesbot.service.session;

import java.util.function.Consumer;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

public interface SessionService {
    @NonNull
    Integer createSession(@NonNull User user, @NonNull Consumer<Message> function,
            boolean isUserOrChatRequestButton);

    void removeSessionsForUser(@NonNull User user);

    void processResponse(@NonNull Message message);
}
