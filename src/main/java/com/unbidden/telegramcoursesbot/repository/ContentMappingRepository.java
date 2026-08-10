package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContentMappingRepository extends JpaRepository<ContentMapping, Long> {
    @EntityGraph(attributePaths = {"content"})
    Optional<ContentMapping> findById(Long id);

    @Query("""
        select c.title
        from Course c 
        left join fetch c.title t
        left join fetch t.content cnt
        where c.id = :courseId
    """)
    Optional<ContentMapping> findCourseTitle(Long courseId);
}
