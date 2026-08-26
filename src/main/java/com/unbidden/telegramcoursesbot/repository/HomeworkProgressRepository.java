package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.HomeworkProgress;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HomeworkProgressRepository extends JpaRepository<HomeworkProgress, Long> {
    @EntityGraph(attributePaths = {"user", "homework", "homework.lesson",
            "homework.lesson.course", "homework.mapping"})
    Optional<HomeworkProgress> findById(Long id);

    @Query("""
        from HomeworkProgress hp
        left join fetch hp.user u
        left join fetch hp.homework h
        left join fetch h.lesson l
        left join fetch l.course c
        where u.id = :userId and h.id = :homeworkId and hp.status <> 'COMPLETED'
        """)
    Optional<HomeworkProgress> findByUserIdAndHomeworkIdUnresolved(Long userId, Long homeworkId);
}
