package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.Video;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VideoRepository extends JpaRepository<Video, String> {
    @Query("""
        select distinct v
        from Video v
        left join fetch v.thumbnail th
        where exists(
            select 1
            from GraphicsContent gc
            left join gc.videos v2
            where gc.id = :contentId and v2 = v
        )
    """)
    List<Video> findByContent(Long contentId);
}
