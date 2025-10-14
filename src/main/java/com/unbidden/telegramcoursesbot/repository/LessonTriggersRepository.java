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
    @Query("from LessonTrigger lt left join fetch lt.user u left join fetch lt.bot b "
            + "left join fetch lt.progress p left join fetch p.course c "
            + "where lt.target < :currentTime")
    List<LessonTrigger> findAllExpired(@NonNull LocalDateTime currentTime);

    @NonNull
    @EntityGraph(attributePaths = {"user", "bot", "progress"})
    Optional<LessonTrigger> findById(@NonNull Long id);

    @NonNull
    @Query("from LessonTrigger lt left join fetch lt.user u left join fetch lt.bot b "
            + "left join fetch lt.progress p left join fetch p.course c where u.id = :userId and "
            + "c.id = :courseId and p.stage = :stage")
    Optional<LessonTrigger> findByCourseStageAndUser(@NonNull Long userId,
            @NonNull Long courseId, @NonNull Integer stage);
}
