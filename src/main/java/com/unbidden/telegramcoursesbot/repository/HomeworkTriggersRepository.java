package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.HomeworkTrigger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface HomeworkTriggersRepository extends JpaRepository<HomeworkTrigger, Long> {
    @Query("""
        from HomeworkTrigger ht
        left join fetch ht.progress p
        left join fetch p.user u
        left join fetch p.homework h
        left join fetch h.mapping m
        left join fetch h.lesson l
        left join fetch l.course c
        where ht.target < :currentTime
    """)
    @NonNull
    List<HomeworkTrigger> findAllExpired(@NonNull LocalDateTime currentTime);

    @NonNull
    @EntityGraph(attributePaths = {"user", "bot", "progress"})
    Optional<HomeworkTrigger> findById(@NonNull Long id);

    boolean existsByUserIdAndProgressHomeworkId(@NonNull Long userId, @NonNull Long homeworkId);
}
