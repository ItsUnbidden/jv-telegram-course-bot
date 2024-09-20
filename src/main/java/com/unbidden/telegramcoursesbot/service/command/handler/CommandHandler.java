package com.unbidden.telegramcoursesbot.service.command.handler;

import org.telegram.telegrambots.meta.api.objects.Message;

public interface CommandHandler {
    void handle(Message message, String[] commandParts);

    String getCommand();
}
