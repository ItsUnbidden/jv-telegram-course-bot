package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface HomeworkProgressRepository extends JpaRepository<HomeworkProgress, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"user", "homework", "content", "homework.lesson",
            "homework.lesson.course", "homework.mapping"})
    Optional<HomeworkProgress> findById(@NonNull Long id);

    @NonNull
    @EntityGraph(attributePaths = {"user", "homework"})
    Optional<HomeworkProgress> findByUserIdAndHomeworkId(@NonNull Long id,
            @NonNull Long homeworkId);

    @NonNull
    @Query("from HomeworkProgress hp left join fetch hp.user u left join fetch hp.homework h"
            + " left join fetch h.lesson l left join fetch l.course c where u.id = :userId and "
            + "h.id = :homeworkId and hp.status != 1")
    Optional<HomeworkProgress> findByUserIdAndHomeworkIdUnresolved(@NonNull Long userId,
            @NonNull Long homeworkId);
}
