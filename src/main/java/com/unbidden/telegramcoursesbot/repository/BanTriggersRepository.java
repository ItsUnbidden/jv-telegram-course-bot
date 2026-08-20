package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.BanTrigger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BanTriggersRepository extends JpaRepository<BanTrigger, Long> {
    @Query("from BanTrigger bt left join fetch bt.user u left join fetch bt.bot b "
            + "where bt.target < :currentTime")
    List<BanTrigger> findAllExpired(LocalDateTime currentTime);

    @EntityGraph(attributePaths = {"user", "bot"})
    Optional<BanTrigger> findById(Long id);

    int deleteByUserIdAndBotId(Long userId, Long botId);
}
