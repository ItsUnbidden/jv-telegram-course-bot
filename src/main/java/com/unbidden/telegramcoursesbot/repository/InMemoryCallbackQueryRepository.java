package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Repository
@RequiredArgsConstructor
public class InMemoryCallbackQueryRepository implements CallbackQueryRepository {
    private static final ConcurrentMap<Key, CallbackQuery> queries =
            new ConcurrentHashMap<>();

    @Override
    @NonNull
    public CallbackQuery save(@NonNull CallbackQuery query, @NonNull UserEntity user,
            @NonNull Bot bot) {
        queries.put(new Key(user, bot), query);
        return query;
    }

    @Override
    public Optional<CallbackQuery> findAndRemove(@NonNull UserEntity user, @NonNull Bot bot) {
        return Optional.ofNullable(queries.remove(new Key(user, bot)));
    }

    @Data
    private static class Key {
        UserEntity user;

        Bot bot;

        public Key(UserEntity user, Bot bot) {
            this.user = user;
            this.bot = bot;
        }
    }
}
