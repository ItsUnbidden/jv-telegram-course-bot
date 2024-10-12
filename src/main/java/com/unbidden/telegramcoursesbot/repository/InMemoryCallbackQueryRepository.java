package com.unbidden.telegramcoursesbot.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Repository
public class InMemoryCallbackQueryRepository implements CallbackQueryRepository {
    private static final Map<Long, CallbackQuery> queries = new HashMap<>();

    @Override
    @NonNull
    public CallbackQuery save(@NonNull CallbackQuery query) {
        queries.put(query.getFrom().getId(), query);
        return query;
    }

    @Override
    @NonNull
    public Optional<CallbackQuery> find(@NonNull Long id) {
        return Optional.ofNullable(queries.get(id));
    }

    @Override
    public Optional<CallbackQuery> findAndRemove(@NonNull Long userId) {
        final CallbackQuery potentialQuery = queries.get(userId);

        if (potentialQuery != null) {
            return Optional.ofNullable(queries.remove(userId));
        }
        return Optional.empty();
    }
}
