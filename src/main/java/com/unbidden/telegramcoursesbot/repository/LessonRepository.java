package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Lesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"course", "structure"})
    List<Lesson> findByCourseName(@NonNull String courseName);

    @NonNull
    @EntityGraph(attributePaths = {"course", "structure", "homework",
            "homework.mapping", "course.bot"})
    Optional<Lesson> findByPositionAndCourseName(@NonNull Integer index,
            @NonNull String courseName);

    @NonNull
    @EntityGraph(attributePaths = {"course", "structure", "homework", "course.bot"})
    Optional<Lesson> findById(@NonNull Long id);
}
