package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import org.springframework.lang.NonNull;

public interface ButtonHandler {
    void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params);
}
