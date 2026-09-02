package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.BotRole;

import java.util.Optional;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackQueryRepository {
    CallbackQuery save(BotRole botRole, CallbackQuery query);

    Optional<CallbackQuery> findAndRemove(BotRole botRole);
}
