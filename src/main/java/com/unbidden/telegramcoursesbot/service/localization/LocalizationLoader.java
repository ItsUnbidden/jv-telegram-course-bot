package com.unbidden.telegramcoursesbot.service.localization;

import java.util.Map;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

public interface LocalizationLoader {
    @NonNull
    String getLocTextForUser(@NonNull String name, @NonNull User user);

    @NonNull
    String getLocTextForUser(@NonNull String name, @NonNull User user,
            Map<String, Object> parameterMap);
            
    @NonNull
    SendMessage getSendMessage(@NonNull String name, @NonNull User user);

    @NonNull
    SendMessage getSendMessage(@NonNull String name, @NonNull User user,
            Map<String, Object> parameterMap);

    void reloadResourses();

    @NonNull
    Localization loadLocalization(@NonNull String name, @NonNull String languageCode);
}
