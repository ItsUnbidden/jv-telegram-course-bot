package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @EntityGraph(attributePaths = {"title", "title.content"})
    List<Course> findByBotId(Long botId);

    @EntityGraph(attributePaths = {"lessons"})
    Optional<Course> findById(Long id);

    @Query("""
        select distinct c
        from Course c
        left join fetch c.title t
        left join fetch t.content tc
        where c.bot.id = :botId and exists(
            select 1
            from CourseOwnership co
            where co.user.id = :userId and co.course = c and co.status = 'ACTIVE'
        )        
    """)
    List<Course> findAllOwnedByUser(Long userId, Long botId);

    @Query("""
        select distinct c
        from Course c
        left join fetch c.title t
        left join fetch t.content tc
        where c.bot.id = :botId and not exists(
            select 1
            from CourseOwnership co
            where co.user.id = :userId and co.course = c and co.status = 'ACTIVE'
        )        
    """)
    List<Course> findAllAvailableToUser(Long userId, Long botId);

    boolean existsByIdAndIsUnderMaintenanceTrue(Long courseId);

    long countByBotId(Long botId);
}
