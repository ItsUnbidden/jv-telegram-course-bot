package com.unbidden.telegramcoursesbot.repository.impl;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.repository.CallbackQueryRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Repository
@RequiredArgsConstructor
public class InMemoryCallbackQueryRepository implements CallbackQueryRepository {
    private static final ConcurrentMap<BotRole, CallbackQuery> queries =
            new ConcurrentHashMap<>();

    @Override
    public CallbackQuery save(BotRole botRole, CallbackQuery query) {
        queries.put(botRole, query);
        return query;
    }

    @Override
    public Optional<CallbackQuery> findAndRemove(BotRole botRole) {
        return Optional.ofNullable(queries.remove(botRole));
    }
}
