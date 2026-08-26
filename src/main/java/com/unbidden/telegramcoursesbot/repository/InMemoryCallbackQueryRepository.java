package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Repository
@RequiredArgsConstructor
public class InMemoryCallbackQueryRepository implements CallbackQueryRepository {
    private static final ConcurrentMap<Key, CallbackQuery> queries =
            new ConcurrentHashMap<>();

    @Override
    public CallbackQuery save(UserEntity user, Bot bot, CallbackQuery query) {
        queries.put(new Key(user, bot), query);
        return query;
    }

    @Override
    public Optional<CallbackQuery> findAndRemove(UserEntity user, Bot bot) {
        return Optional.ofNullable(queries.remove(new Key(user, bot)));
    }

    @Data
    private static class Key {
        Long userId;

        Bot bot;

        public Key(UserEntity user, Bot bot) {
            this.userId = user.getId();
            this.bot = bot;
        }
    }
}
