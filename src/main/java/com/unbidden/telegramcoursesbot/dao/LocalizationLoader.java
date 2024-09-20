package com.unbidden.telegramcoursesbot.dao;

import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.User;

public interface LocalizationLoader {
    @NonNull
    String getTextByNameForUser(@NonNull String name, @NonNull User user);
}
