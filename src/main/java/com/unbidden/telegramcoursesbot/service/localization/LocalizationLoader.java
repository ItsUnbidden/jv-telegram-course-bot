package com.unbidden.telegramcoursesbot.service.localization;

import java.util.Map;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.User;

public interface LocalizationLoader {
    @NonNull
    Localization getLocalizationForUser(@NonNull String name, @NonNull User user);

    @NonNull
    Localization getLocalizationForUser(@NonNull String name, @NonNull User user,
            Map<String, Object> parameterMap);

    void reloadResourses();

    @NonNull
    Localization loadLocalization(@NonNull String name, @NonNull String languageCode);
}
