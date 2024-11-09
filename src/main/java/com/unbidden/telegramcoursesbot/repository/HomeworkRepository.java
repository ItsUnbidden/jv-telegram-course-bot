package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Homework;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"lesson", "lesson.course", "mapping", "mapping.content"})
    Optional<Homework> findById(@NonNull Long id);
}
