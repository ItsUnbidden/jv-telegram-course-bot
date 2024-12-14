package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.DocumentContent;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface DocumentContentRepository extends JpaRepository<DocumentContent, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"documents", "bot"})
    Optional<DocumentContent> findById(@NonNull Long id);
}
