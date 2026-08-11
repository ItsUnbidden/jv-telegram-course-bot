package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

public interface ButtonHandler {
    void handle(UserEntity user, Bot bot, String[] params);
}
