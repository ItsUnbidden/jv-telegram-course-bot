package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Review;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @EntityGraph(attributePaths = {"user", "course"})
    Optional<Review> findById(Long userId);

    @EntityGraph(attributePaths = {"user", "course"})
    Page<Review> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "course"})
    Page<Review> findByCourseId(Long courseId, Pageable pageable);
    
    @Query("""
        from Review r
        left join fetch r.user u
        left join fetch r.course c
        left join fetch r.commentedBy cb
        where c.bot.id = :botId and exists(
            select 1
            from Review r2
            left join r.markedAsRead marked
            where r2.id = r.id and :userId not in elements(marked.id))
    """)
    Page<Review> findNewReviewsForUser(Long userId, Long botId, Pageable pageable);

    @Query("""
        from Review r
        left join fetch r.user u
        left join fetch r.course c
        left join fetch r.commentedBy cb
        where c.id = :courseId and exists(
            select 1
            from Review r2
            left join r.markedAsRead marked
            where r2.id = r.id and :userId not in elements(marked.id))
    """)
    Page<Review> findNewReviewsForUserAndCourse(Long userId, Long courseId, Pageable pageable);

    @Query("""
        select distinct r
        from Review r
        left join fetch r.user u
        left join fetch r.course c
        left join fetch r.commentedBy cb
        left join fetch r.content cnt
        left join fetch r.markedAsRead marked
        where c.bot.id = :botId and :userId in elements(marked.id)
    """)
    List<Review> findArchiveReviewsForUser(Long userId, Long botId);

    @Query("""
        select distinct r
        from Review r
        left join fetch r.user u
        left join fetch r.course c
        left join fetch r.commentedBy cb
        left join fetch r.content cnt
        left join fetch r.markedAsRead marked
        where c.id = :courseId and :userId in elements(marked.id)
    """)
    List<Review> findArchiveReviewsForUserAndCourse(Long userId, Long courseId);

    @Query("""
        select distinct r
        from Review r
        left join fetch r.markedAsReadBy markedBy
        where r.id in :reviewIds        
    """)
    List<Review> findByIdIn(List<Long> reviewIds);
    
    @EntityGraph(attributePaths = {"user", "course", "markedAsReadBy"})
    Optional<Review> findByCourseIdAndUserId(Long courseId, Long userId);

    boolean existsByCourseIdAndUserId(Long courseId, Long userId);

    boolean existsByCourseIdAndUserIdAndContentIsNotNull(Long courseId, Long userId);
}
