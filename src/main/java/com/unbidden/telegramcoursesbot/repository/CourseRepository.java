package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @EntityGraph(attributePaths = {"lessons"})
    List<Course> findByBotId(Long botId);

    @EntityGraph(attributePaths = {"lessons"})
    Optional<Course> findById(Long id);

    @Query("""
        from Course c
        where c.bot.id = :botId and exists(
            select 1
            from PaymentDetails pd
            where pd.user.id = :userId and pd.isValid
        )        
    """)
    List<Course> findAllOwnedByUser(Long userId, Long botId);

    boolean existsByIdAndIsUnderMaintenanceTrue(Long courseId);

    long countByBotId(Long botId);
}
