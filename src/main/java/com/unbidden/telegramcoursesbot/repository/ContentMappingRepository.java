package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.dto.internal.MappingsByPositionInCourseCountDto;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContentMappingRepository extends JpaRepository<ContentMapping, Long> {
    @EntityGraph(attributePaths = {"content"})
    Optional<ContentMapping> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"content"})
    List<ContentMapping> findAllById(Iterable<Long> ids);

    @Query("""
        select c.title
        from Course c 
        left join c.title t
        left join fetch t.content cnt
        where c.id = :courseId
    """)
    Optional<ContentMapping> findCourseTitle(Long courseId);

    @Query("""
        select new com.unbidden.telegramcoursesbot.dto.internal.MappingsByPositionInCourseCountDto(l.position, count(s.id))
        from Lesson l
        left join l.structure s
        where l.course.id = :courseId
        group by l.position
    """)
    List<MappingsByPositionInCourseCountDto> countAndGroupByPositionInLessonsInCourse(Long courseId);
}
