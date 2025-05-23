package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface SessionDistributor {
    void callService(@NonNull Message message);

    void removeSessionsForUser(@NonNull UserEntity user);

    void removeSessionsWithoutConfirmationForUser(@NonNull UserEntity user);
}
