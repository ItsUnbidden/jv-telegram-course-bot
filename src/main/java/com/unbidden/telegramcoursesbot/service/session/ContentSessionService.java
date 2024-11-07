package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface ContentSessionService extends SessionService {
    Integer createSession(@NonNull UserEntity user, @NonNull Consumer<List<Message>> function,
            boolean isSkippingConfirmation);
    
    void removeSessionsWithoutConfirmationForUser(@NonNull UserEntity user);

    void commit(@NonNull Integer sessionId);

    void resend(@NonNull Integer sessionId);

    void cancel(@NonNull Integer sessionId);
}
