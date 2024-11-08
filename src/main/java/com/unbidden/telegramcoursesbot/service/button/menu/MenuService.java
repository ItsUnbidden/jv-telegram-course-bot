package com.unbidden.telegramcoursesbot.service.button.menu;

import com.unbidden.telegramcoursesbot.model.MenuTerminationGroup;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.User;

public interface MenuService {
    @NonNull
    Message initiateMenu(@NonNull String menuName, @NonNull User user);

    @NonNull
    Message initiateMenu(@NonNull String menuName, @NonNull UserEntity user);

    void initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull Integer messageId);

    @NonNull
    Message initiateMenu(@NonNull String menuName, @NonNull User user, @NonNull String param);

    @NonNull
    Message initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull String param);

    void initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull String param, @NonNull Integer messageId);

    void processCallbackQuery(@NonNull CallbackQuery query);

    @NonNull
    Menu save(@NonNull Menu menu);

    @NonNull
    MenuTerminationGroup addToMenuTerminationGroup(@NonNull UserEntity user,
            @NonNull UserEntity messagedUser, @NonNull Integer messageId, @NonNull String key,
            @Nullable String terminalLocalizationName);

    void terminateMenuGroup(@NonNull UserEntity user, @NonNull String key);
    
    void terminateMenu(@NonNull Long chatId, @NonNull Integer messageId,
            @Nullable Localization terminalPageLocalization);

    void terminateMenu(@NonNull Long chatId, @NonNull Integer messageId);
}