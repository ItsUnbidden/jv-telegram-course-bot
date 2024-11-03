package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface ContentMappingRepository extends JpaRepository<ContentMapping, Long> {
    @NonNull
    @EntityGraph(attributePaths = "content")
    Optional<ContentMapping> findById(@NonNull Long id);
}
