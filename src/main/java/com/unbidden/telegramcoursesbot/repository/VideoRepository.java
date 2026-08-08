package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.Video;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VideoRepository extends JpaRepository<Video, String> {
    @Query("""
        select c.videos
        from GraphicsContent c
        left join fetch c.videos.thumbnail th
        where c.id = :contentId
    """)
    List<Video> findByContent(Long contentId);
}
