package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Lesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    @EntityGraph(attributePaths = {"course", "structure"})
    List<Lesson> findByCourseIdOrderByPosition(Long courseId);

    @EntityGraph(attributePaths = {"course", "structure", "homework"})
    Optional<Lesson> findByPositionAndCourseId(Integer index, Long courseId);

    @EntityGraph(attributePaths = {"course", "structure", "homework"})
    Optional<Lesson> findById(Long id);
}
