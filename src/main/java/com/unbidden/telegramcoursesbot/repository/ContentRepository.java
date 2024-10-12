package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Content;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface ContentRepository extends JpaRepository<Content, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"video", "photos"})
    Optional<Content> findById(@NonNull Long id);
}
