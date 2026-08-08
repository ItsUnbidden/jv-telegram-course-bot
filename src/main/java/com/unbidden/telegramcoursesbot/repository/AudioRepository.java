package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.Audio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AudioRepository extends JpaRepository<Audio, String> {
    @Query("""
        select c.audios
        from AudioContent c
        left join fetch c.audios.thumbnail th
        where c.id = :contentId
    """)
    List<Audio> findByContent(Long contentId);
}
