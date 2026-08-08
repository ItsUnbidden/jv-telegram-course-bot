package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {
    @EntityGraph(attributePaths = {"user", "course", "course.lessons"})
    Optional<CourseProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    @EntityGraph(attributePaths = {"user", "course", "course.lessons"})
    Optional<CourseProgress> findById(Long id);

    @EntityGraph(attributePaths = {"user"})
    List<CourseProgress> findByCourseIdAndNumberOfTimesCompletedGreaterThan(Long courseId,
            Integer numberOfTimesCompleted, Pageable pageable);

    long countByCourseAndNumberOfTimesCompletedGreaterThan(Course course, Integer numberOfTimesCompleted);

    @EntityGraph(attributePaths = {"user"})
    List<CourseProgress> findByCourseIdAndStageAndNumberOfTimesCompleted(Long courseId, Integer stage,
            Integer numberOfTimesCompleted, Pageable pageable);

    long countByCourseIdAndStageAndNumberOfTimesCompleted(Long courseId, Integer stage,
            Integer numberOfTimesCompleted);
}
