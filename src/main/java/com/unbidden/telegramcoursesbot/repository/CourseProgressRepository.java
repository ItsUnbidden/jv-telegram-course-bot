package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.CourseProgress;
import java.util.Optional;
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
}
