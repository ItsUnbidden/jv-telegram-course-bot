package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface BotRepository extends JpaRepository<Bot, Long> {
    @NonNull
    Optional<Bot> findById(@NonNull Long id);

    @NonNull
    Optional<Bot> findByName(@NonNull String name);
}
