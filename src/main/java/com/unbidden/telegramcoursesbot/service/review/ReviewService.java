package com.unbidden.telegramcoursesbot.service.review;

import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

public interface ReviewService {
    void initiateBasicReview(@NonNull UserEntity user, @NonNull Course course);

    void initiateAdvancedReview(@NonNull Review review, @NonNull Integer messageId);

    @NonNull
    Review commitBasicReview(@NonNull UserEntity user, @NonNull Course course,
            int courseGrade, int platformGrade);

    @NonNull
    Review commitAdvancedReview(@NonNull Long reviewId, @NonNull LocalizedContent content);

    @NonNull
    Review leaveComment(@NonNull UserEntity user, @NonNull Review review,
            @NonNull LocalizedContent content);

    @NonNull
    Review updateCourseGrade(@NonNull Long reviewId, int newGrade);

    @NonNull
    Review updatePlatformGrade(@NonNull Long reviewId, int newGrade);

    @NonNull
    Review updateAdvancedReview(@NonNull Long reviewId, @NonNull LocalizedContent content);

    @NonNull
    List<Review> getReviewsForCourse(@NonNull Course course, Pageable pageable);

    void sendNewReviewsForUser(@NonNull UserEntity user);

    void sendNewReviewsForUserAndCourse(@NonNull UserEntity user, @NonNull Long courseId);

    void sendArchiveReviewsForUser(@NonNull UserEntity user);

    void sendArchiveReviewsForUserAndCourse(@NonNull UserEntity user, @NonNull Long courseId);

    @NonNull
    Review getReviewByCourseAndUser(@NonNull UserEntity user, @NonNull Course course);

    @NonNull
    Review getReviewById(@NonNull Long reviewId);

    boolean isBasicReviewForCourseAndUserAvailable(@NonNull UserEntity user,
            @NonNull Course course);

    boolean isAdvancedReviewForCourseAndUserAvailable(@NonNull UserEntity user,
            @NonNull Course course);

    @NonNull
    Review markReviewAsRead(@NonNull Review review, @NonNull UserEntity user);
}
