package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.LessonTrigger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LessonTriggersRepository extends JpaRepository<LessonTrigger, Long> {
    @Query("""
        from LessonTrigger lt
        left join fetch lt.progress p
        left join fetch lt.botRole br
        left join fetch br.user u
        left join fetch br.bot b
        where lt.target < :currentTime
    """)
    List<LessonTrigger> findAllExpired(LocalDateTime currentTime);

    @EntityGraph(attributePaths = {"botRole", "botRole.user", "progress"})
    Optional<LessonTrigger> findById(Long id);

    Optional<LessonTrigger> findByBotRoleIdAndProgressCourseIdAndProgressStage(Long botRoleId,
            Long courseId, Integer stage);
}
