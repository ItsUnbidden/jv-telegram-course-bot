package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.LessonTrigger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface LessonTriggersRepository extends JpaRepository<LessonTrigger, Long> {
    @NonNull
    @Query("""
           from LessonTrigger lt
           left join fetch lt.progress p
           left join fetch p.user u
           where lt.target < :currentTime
    """)
    List<LessonTrigger> findAllExpired(@NonNull LocalDateTime currentTime);

    @NonNull
    @EntityGraph(attributePaths = {"user", "progress"})
    Optional<LessonTrigger> findById(@NonNull Long id);

    @NonNull
    Optional<LessonTrigger> findByUserIdAndCourseIdAndProgressStage(@NonNull Long userId,
            @NonNull Long courseId, @NonNull Integer stage);
}
