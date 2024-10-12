package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.function.Consumer;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface SessionService {
    @NonNull
    Integer createSession(@NonNull UserEntity user, boolean isUserOrChatRequestButton,
            @NonNull Consumer<Message> function);

    void removeSessionsForUser(@NonNull UserEntity user);

    void processResponse(@NonNull Message message);
}
