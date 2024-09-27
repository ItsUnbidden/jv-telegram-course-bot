package com.unbidden.telegramcoursesbot.service.localization;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.User;

public interface LocalizationLoader {
    @NonNull
    Localization getLocalizationForUser(@NonNull String name, @NonNull User user);

    @NonNull
    Localization getLocalizationForUser(@NonNull String name, @NonNull UserEntity user);

    @NonNull
    Localization getLocalizationForUser(@NonNull String name, @NonNull User user,
            @NonNull Map<String, Object> parameterMap);

    @NonNull
    Localization getLocalizationForUser(@NonNull String name, @NonNull UserEntity user,
            @NonNull Map<String, Object> parameterMap);

    @NonNull
    Localization getLocalizationForUser(@NonNull String name, @NonNull User user,
            @NonNull String paramKey, @NonNull Object param);

    void reloadResourses();

    @NonNull
    Localization loadLocalization(@NonNull String name, @NonNull String languageCode);
}
