package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import org.springframework.lang.NonNull;

public interface ButtonHandler {
    void handle(@NonNull UserEntity user, @NonNull String[] params);
}
