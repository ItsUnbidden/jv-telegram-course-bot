package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"user", "course", "markedAsReadBy", "content",
            "commentContent"})
    Optional<Review> findById(@NonNull Long userId);

    @NonNull
    @EntityGraph(attributePaths = {"user", "course", "markedAsReadBy", "content",
            "commentContent"})
    List<Review> findByUserId(@NonNull Long userId, Pageable pageable);

    @NonNull
    @EntityGraph(attributePaths = {"user", "course", "markedAsReadBy", "content",
            "commentContent"})
    List<Review> findByCourseId(@NonNull Long courseId, Pageable pageable);
    
    @NonNull
    @Query("from Review r left join fetch r.user u left join fetch r.course c "
            + "left join fetch r.commentContent cc left join fetch r.markedAsReadBy"
            + " mr where :user not in elements(mr)")
    List<Review> findNewReviewsForUser(@NonNull UserEntity user, Pageable pageable);

    @NonNull
    @Query("from Review r left join fetch r.user u left join fetch r.course c "
            + "left join fetch r.commentContent cc left join fetch r.markedAsReadBy"
            + " mr where c.id = :courseId and :user not in elements(mr)")
    List<Review> findNewReviewsForUserAndCourse(@NonNull UserEntity user, @NonNull Long courseId,
            Pageable pageable);

    @NonNull
    @Query("from Review r left join fetch r.user u left join fetch r.course c "
            + "left join fetch r.commentContent cc left join fetch r.markedAsReadBy"
            + " mr where :user in elements(mr)")
    List<Review> findArchiveReviewsForUser(@NonNull UserEntity user);

    @NonNull
    @Query("from Review r left join fetch r.user u left join fetch r.course c "
            + "left join fetch r.commentContent cc left join fetch r.markedAsReadBy"
            + " mr where c.id = :courseId and :user in elements(mr)")
    List<Review> findArchiveReviewsForUserAndCourse(@NonNull UserEntity user,
            @NonNull Long courseId);

    @NonNull
    @EntityGraph(attributePaths = {"user", "course", "markedAsReadBy", "content",
            "commentContent"})
    Optional<Review> findByCourseNameAndUserId(@NonNull String courseName, @NonNull Long userId);
}
