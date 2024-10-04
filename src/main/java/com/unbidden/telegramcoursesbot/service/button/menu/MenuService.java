package com.unbidden.telegramcoursesbot.service.button.menu;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
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

    void terminateMenu(@NonNull Long chatId, @NonNull Integer messageId,
            Localization terminalPageLocalization);
}