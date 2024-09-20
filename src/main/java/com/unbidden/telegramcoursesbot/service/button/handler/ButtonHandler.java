package com.unbidden.telegramcoursesbot.service.button.handler;

import org.telegram.telegrambots.meta.api.objects.User;

public interface ButtonHandler {
    void handle(String[] params, User user);
}
