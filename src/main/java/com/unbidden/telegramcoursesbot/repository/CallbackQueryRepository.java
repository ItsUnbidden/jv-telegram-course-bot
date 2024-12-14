package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackQueryRepository {
    CallbackQuery save(@NonNull CallbackQuery query, @NonNull UserEntity user, @NonNull Bot bot);

    Optional<CallbackQuery> findAndRemove(@NonNull UserEntity user, @NonNull Bot bot);
}
