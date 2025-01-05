package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {
    @NonNull
    @Query("from CourseProgress cp left join fetch cp.user u left join fetch cp.course c "
            + " left join fetch c.lessons l left join fetch c.bot b where u.id = :userId " 
            + "and c.name = :courseName")
    Optional<CourseProgress> findByUserIdAndCourseName(@NonNull Long userId,
            @NonNull String courseName);

    @NonNull
    @EntityGraph(attributePaths = {"user", "course", "course.lessons", "course.bot"})
    Optional<CourseProgress> findById(@NonNull Long id);

    @NonNull
    @EntityGraph(attributePaths = {"user"})
    List<CourseProgress> findByCourseAndNumberOfTimesCompletedGreaterThan(
            @NonNull Course course, @NonNull Integer numberOfTimesCompleted,
            @NonNull Pageable pageable);

    long countByCourseAndNumberOfTimesCompletedGreaterThan(@NonNull Course course,
            @NonNull Integer numberOfTimesCompleted);

    @NonNull
    @EntityGraph(attributePaths = {"user"})
    List<CourseProgress> findByCourseAndStageAndNumberOfTimesCompleted(
            @NonNull Course course, @NonNull Integer stage,
            @NonNull Integer numberOfTimesCompleted,
            @NonNull Pageable pageable);

    long countByCourseAndStageAndNumberOfTimesCompleted(@NonNull Course course,
            @NonNull Integer stage, @NonNull Integer numberOfTimesCompleted);
}
