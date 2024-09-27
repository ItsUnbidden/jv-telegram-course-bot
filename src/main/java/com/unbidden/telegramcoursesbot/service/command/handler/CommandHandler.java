package com.unbidden.telegramcoursesbot.service.command.handler;

import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface CommandHandler {
    void handle(@NonNull Message message, @NonNull String[] commandParts);

    @NonNull
    String getCommand();
}
