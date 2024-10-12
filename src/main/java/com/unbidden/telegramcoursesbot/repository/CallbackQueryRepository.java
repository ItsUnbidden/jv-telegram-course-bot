package com.unbidden.telegramcoursesbot.repository;

import java.util.Optional;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackQueryRepository extends CustomGeneralRepository<Long, CallbackQuery> {
    Optional<CallbackQuery> findAndRemove(@NonNull Long userId);
}
