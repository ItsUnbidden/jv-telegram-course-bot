package com.unbidden.telegramcoursesbot.service.review;

import com.unbidden.telegramcoursesbot.model.Bot;
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
    Review commitAdvancedReview(@NonNull Long reviewId, @NonNull UserEntity user,
            @NonNull LocalizedContent content);

    @NonNull
    Review leaveComment(@NonNull UserEntity user, @NonNull Review review,
            @NonNull LocalizedContent content);

    @NonNull
    Review updateComment(@NonNull UserEntity user, @NonNull Review review,
            @NonNull LocalizedContent content);

    @NonNull
    Review updateCourseGrade(@NonNull Long reviewId, @NonNull UserEntity user,
            @NonNull Bot bot, int newGrade);

    @NonNull
    Review updatePlatformGrade(@NonNull Long reviewId, @NonNull UserEntity user,
            @NonNull Bot bot, int newGrade);

    @NonNull
    Review updateAdvancedReview(@NonNull Long reviewId, @NonNull UserEntity user,
            @NonNull LocalizedContent content);

    @NonNull
    List<Review> getReviewsForCourse(@NonNull Course course, Pageable pageable);

    void sendNewReviewsForUser(@NonNull UserEntity user, @NonNull Bot bot);

    void sendNewReviewsForUserAndCourse(@NonNull UserEntity user,
            @NonNull Long courseId, @NonNull Bot bot);

    void sendArchiveReviewsForUser(@NonNull UserEntity user, @NonNull Bot bot);

    void sendArchiveReviewsForUserAndCourse(@NonNull UserEntity user,
            @NonNull Long courseId, @NonNull Bot bot);

    @NonNull
    Review getReviewByCourseAndUser(@NonNull UserEntity user, @NonNull Course course);

    @NonNull
    Review getReviewById(@NonNull Long reviewId, @NonNull UserEntity user, @NonNull Bot bot);

    boolean isBasicReviewForCourseAndUserAvailable(@NonNull UserEntity user,
            @NonNull Course course);

    boolean isAdvancedReviewForCourseAndUserAvailable(@NonNull UserEntity user,
            @NonNull Course course);

    @NonNull
    Review markReviewAsRead(@NonNull Review review, @NonNull UserEntity user);
}
