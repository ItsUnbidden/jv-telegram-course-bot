package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackQueryRepository {
    CallbackQuery save(UserEntity user, Bot bot, CallbackQuery query);

    Optional<CallbackQuery> findAndRemove(UserEntity user, Bot bot);
}
