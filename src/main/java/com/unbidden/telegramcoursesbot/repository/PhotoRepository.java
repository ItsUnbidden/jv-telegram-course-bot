package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.Photo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PhotoRepository extends JpaRepository<Photo, String> {
    @Query("""
        select c.photos
        from GraphicsContent c
        where c.id = :contentId
    """)
    List<Photo> findByContent(Long contentId);
}
