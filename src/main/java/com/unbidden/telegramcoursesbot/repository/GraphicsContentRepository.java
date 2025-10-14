package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.GraphicsContent;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface GraphicsContentRepository extends JpaRepository<GraphicsContent, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"photos", "videos", "bot"})
    Optional<GraphicsContent> findById(@NonNull Long id);
}
