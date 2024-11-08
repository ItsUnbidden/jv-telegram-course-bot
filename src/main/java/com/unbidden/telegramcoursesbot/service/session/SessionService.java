package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
public interface SessionService {
    @NonNull
    Integer createSession(@NonNull UserEntity user, @NonNull Consumer<List<Message>> function);

    void removeSessionsForUser(@NonNull UserEntity user);

    void processResponse(@NonNull Session session, @NonNull Message message);
}
