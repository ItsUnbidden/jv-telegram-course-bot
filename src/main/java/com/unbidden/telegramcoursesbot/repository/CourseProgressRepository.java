package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.dto.internal.UsersByCourseStageCountDto;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {
    @EntityGraph(attributePaths = {"user", "course", "course.lessons"})
    Optional<CourseProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    @EntityGraph(attributePaths = {"user", "course", "course.lessons"})
    Optional<CourseProgress> findById(Long id);

    long countByCourseIdAndNumberOfTimesCompletedGreaterThan0(Long courseId);

    @EntityGraph(attributePaths = {"user"})
    List<CourseProgress> findByCourseIdAndStageAndNumberOfTimesCompleted(Long courseId, Integer stage,
            Integer numberOfTimesCompleted, Pageable pageable);

    List<CourseProgress> findByUserIdAndCourseIdIn(Long userId, List<Long> courseIds);

    @Query("""
        select new com.unbidden.telegramcoursesbot.dto.internal.UsersByCourseStageCountDto(cp.stage, count(cp.id))
        from CourseProgress cp
        where cp.course.id = :courseId and cp.numberOfTimesCompleted < 1
        group by cp.stage
        order by cp.stage ASC
    """)
    List<UsersByCourseStageCountDto> countAndGroupByCourseStage(Long courseId);
}
