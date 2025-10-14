package com.unbidden.telegramcoursesbot.service.menu;

import com.unbidden.telegramcoursesbot.exception.CallbackQueryAnswerException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.MenuTerminationGroup;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface MenuService {
    @NonNull
    Message initiateMenu(@NonNull String menuName, @NonNull UserEntity user, @NonNull Bot bot);

    void initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull Integer messageId, @NonNull Bot bot);

    @NonNull
    Message initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull String param, @NonNull Bot bot);

    void initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull String param, @NonNull Integer messageId, @NonNull Bot bot);

    void processCallbackQuery(@NonNull CallbackQuery query, @NonNull Bot bot);

    @NonNull
    Message initiateMultipageList(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull Function<Map<String, Object>, Localization> localizationFunction,
            @NonNull BiFunction<Integer, Integer, List<String>> dataFunction,
            @NonNull Supplier<Long> totalAmountOfElementsSupplier);
            
    void processMultipageListRequest(@NonNull MultipageListMeta meta);

    @NonNull
    MultipageListMeta getMultipageListMeta(@NonNull Integer id, @NonNull UserEntity user);

    @NonNull
    Menu save(@NonNull Menu menu);

    @NonNull
    MenuTerminationGroup addToMenuTerminationGroup(@NonNull UserEntity user,
            @NonNull UserEntity messagedUser, @NonNull Bot bot, @NonNull Integer messageId,
            @NonNull String key, @Nullable String terminalLocalizationName);

    void terminateMenuGroup(@NonNull UserEntity user, @NonNull Bot bot, @NonNull String key);
    
    void terminateMenuGroup(@NonNull UserEntity user, @NonNull Bot bot, @NonNull String key,
            @Nullable Localization terminalLocalizationOverride);
    
    void terminateMenu(@NonNull Long chatId, @NonNull Integer messageId, @NonNull Bot bot,
            @Nullable Localization terminalPageLocalization);

    void terminateMenu(@NonNull Long chatId, @NonNull Integer messageId, @NonNull Bot bot);

    void answerPotentialCallbackQuery(@NonNull UserEntity user, @NonNull Bot bot)
            throws CallbackQueryAnswerException;
}