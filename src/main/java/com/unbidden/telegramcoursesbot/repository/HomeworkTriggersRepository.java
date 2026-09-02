package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.HomeworkTrigger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HomeworkTriggersRepository extends JpaRepository<HomeworkTrigger, Long> {
    @Query("""
        from HomeworkTrigger ht
        left join fetch ht.progress p
        left join fetch ht.botRole br
        left join fetch br.user u
        left join fetch br.bot b
        left join fetch p.homework h
        left join fetch h.mapping m
        left join fetch h.lesson l
        left join fetch l.course c
        where ht.target < :currentTime
    """)
    List<HomeworkTrigger> findAllExpired(LocalDateTime currentTime);

    @EntityGraph(attributePaths = {"botRole", "botRole.user", "progress"})
    Optional<HomeworkTrigger> findById(Long id);

    boolean existsByBotRoleIdAndProgressHomeworkId(Long botRoleId, Long homeworkId);
}
