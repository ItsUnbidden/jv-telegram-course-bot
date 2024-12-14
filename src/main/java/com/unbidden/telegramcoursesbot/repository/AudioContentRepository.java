package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.AudioContent;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface AudioContentRepository extends JpaRepository<AudioContent, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"audios", "bot"})
    Optional<AudioContent> findById(@NonNull Long id);
}
