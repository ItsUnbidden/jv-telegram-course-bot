package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BotRepository extends JpaRepository<Bot, Long> {
    Optional<Bot> findById(Long id);

    @Query("""
        from Bot b
        where b.id <> 1        
    """)
    List<Bot> findAllRegularBots();
}
