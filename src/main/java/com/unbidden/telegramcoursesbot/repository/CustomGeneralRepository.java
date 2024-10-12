package com.unbidden.telegramcoursesbot.repository;

import java.util.Optional;
import org.springframework.lang.NonNull;

public interface CustomGeneralRepository<K, T> {
    @NonNull
    T save(@NonNull T type);

    @NonNull
    Optional<T> find(@NonNull K id);
}
