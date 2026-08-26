package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.Audio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AudioRepository extends JpaRepository<Audio, String> {
    @Query("""
        select distinct a
        from Audio a
        where exists(
            select 1
            from AudioContent ac
            left join ac.audios a2
            where ac.id = :contentId and a2 = a
        )
    """)
    List<Audio> findByContent(Long contentId);
}
