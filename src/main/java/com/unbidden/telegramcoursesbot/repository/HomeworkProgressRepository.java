package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface HomeworkProgressRepository extends JpaRepository<HomeworkProgress, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"user", "homework", "content", "approveMessages",
            "approveMessages.user", "sendHomeworkMessages", "sendHomeworkMessages.user",
            "homework.lesson", "homework.lesson.course", "homework.content"})
    Optional<HomeworkProgress> findById(@NonNull Long id);

    @NonNull
    @Query("from HomeworkProgress hp left join fetch hp.user u left join fetch hp.homework h "
            + "left join fetch hp.sendHomeworkMessages shm where u.id = :userId and h.id = "
            + ":homeworkId and hp.status != 0 and hp.status != 1 and hp.status != 2")
    Optional<HomeworkProgress> findByUserIdAndHomeworkIdUnresolved(@NonNull Long userId,
            @NonNull Long homeworkId);
}
