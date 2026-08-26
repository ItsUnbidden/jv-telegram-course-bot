package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.Photo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PhotoRepository extends JpaRepository<Photo, String> {
    @Query("""
        select distinct ph
        from Photo ph
        where exists(
            select 1
            from GraphicsContent gc
            left join gc.photos ph2
            where gc.id = :contentId and ph2 = ph
        )
    """)
    List<Photo> findByContent(Long contentId);
}
