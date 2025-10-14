package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import java.util.List;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface CommandHandler {
    void handle(@NonNull Bot bot, @NonNull UserEntity user,
            @NonNull Message message, @NonNull String[] commandParts);

    @NonNull
    String getCommand();

    @NonNull
    List<AuthorityType> getAuthorities();
}
