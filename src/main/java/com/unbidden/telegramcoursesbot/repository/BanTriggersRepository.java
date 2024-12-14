package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.BanTrigger;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface BanTriggersRepository extends JpaRepository<BanTrigger, Long> {
    @NonNull
    @Query("from BanTrigger bt left join fetch bt.user u left join fetch bt.bot b "
            + "where bt.target < :currentTime")
    List<BanTrigger> findAllExpired(@NonNull LocalDateTime currentTime);

    @NonNull
    @EntityGraph(attributePaths = {"user", "bot"})
    Optional<BanTrigger> findById(@NonNull Long id);

    @NonNull
    @EntityGraph(attributePaths = {"user", "bot"})
    Optional<BanTrigger> findByBotAndUser(@NonNull Bot bot, @NonNull UserEntity user);
}
