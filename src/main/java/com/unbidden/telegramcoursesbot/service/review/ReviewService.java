package com.unbidden.telegramcoursesbot.service.review;

import com.unbidden.telegramcoursesbot.exception.AccessDeniedException;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.repository.ReviewRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private static final Logger LOGGER = LogManager.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    @Transactional
    public Review createNewReview(UserEntity user, Long courseId, int courseGrade) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.state(courseGrade > 0 && courseGrade <= 10, "courseGrade must be an int between 1 and 10");    

        final Review review = new Review();

        review.setUser(user);
        review.setCourse(entityUtil.getCourseReference(courseId));
        review.setBasicSubmittedTimestamp(LocalDateTime.now());
        review.setOriginalCourseGrade(courseGrade);
        review.setCourseGrade(courseGrade);
        review.setMarkedAsReadBy(Set.of());

        return reviewRepository.save(review);
    }

    @Transactional
    public Review upgradeReview(UserEntity user, Bot bot, Long reviewId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = entityUtil.getReviewById(user, bot, reviewId);

        LOGGER.info("User " + user.getFullName() + " wants to submit an advanced review "
                + "for course " + review.getCourse().getId() + ".");  
        if (review.getContent() != null) {
            throw new ActionExpiredException("Unable to submit content for basic review "
                    + reviewId + " because it already has some content",
                    localizationLoader.localize(
                    Error.COMMIT_ADVANCED_REVIEW_FAILURE, review.getUser()));
        }
        final LocalizedContent content = contentService.parseAndPersistContent(user, bot, messages);

        review.setAdvancedSubmittedTimestamp(LocalDateTime.now());
        review.setOriginalContent(content);
        review.setContent(content);

        return review;
    }

    @Transactional
    public Review leaveComment(UserEntity user, Bot bot, Long reviewId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = entityUtil.getReviewById(user, bot, reviewId);

        LOGGER.info("User " + user.getFullName() + " wants to comment review " + review.getId() + ".");
        if (review.getCommentContent() != null) {
            throw new ActionExpiredException("Unable to submit comment content for review "
                    + review.getId() + " because this review already has a comment from user "
                    + review.getCommentedBy().getFullName(), localizationLoader.localize(
                    Error.LEAVE_COMMENT_FAILURE, user));
        }
        
        review.setCommentContent(contentService.parseAndPersistContent(user, bot, messages));
        review.setCommentedBy(user);
        review.setCommentedAt(LocalDateTime.now());
        review.getMarkedAsReadBy().add(user);

        return review;
    }

    @Transactional
    public Review updateComment(UserEntity user, Bot bot, Long reviewId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = entityUtil.getReviewById(user, bot, reviewId);

        if (review.getCommentContent() == null) {
            throw new ForbiddenOperationException("Review " + review.getId() + "'s comment "
                    + "cannot be updated because it has never been submitted", localizationLoader
                    .localize(Error.UPDATE_COMMENT_FAILURE, user));
        }
        if (!review.getCommentedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Review " + review.getId() + "'s comment "
                    + "has been made by user " + review.getCommentedBy().getFullName()
                    + ". User " + user.getFullName() + " cannot edit it.", localizationLoader
                    .localize(Error.UPDATE_COMMENT_FORBIDDEN, user));
        }
        
        LOGGER.info("User " + user.getId() + " wants to update comment in review "
                + review.getId() + ".");
    
        review.setCommentContent(contentService.parseAndPersistContent(user, bot, messages));
        review.setCommentedAt(LocalDateTime.now());
        review.getMarkedAsReadBy().add(user);

        return review;
    }

    @Transactional
    public Review updateCourseGrade(UserEntity user, Bot bot, Long reviewId, int newGrade) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.state(newGrade > 0 && newGrade <= 10, "newGrade must be an int between 1 and 10");

        final Review review = entityUtil.getReviewById(user, bot, reviewId);

        LOGGER.info("User " + review.getUser().getFullName()
                + " wants to update their grade for course " + review.getCourse().getId()
                + ". Current grade is " + review.getCourseGrade() + " and new grade is "
                + newGrade + ".");
        if (review.getCourseGrade() == newGrade) {
            throw new InvalidDataSentException("New course grade is the same as before",
                    localizationLoader.localize(Error.SAME_NEW_COURSE_GRADE,
                    user, new Error.SameNewCourseGradeParams(newGrade)));
        }
        
        review.setCourseGrade(newGrade);
        review.setLastUpdateTimestamp(LocalDateTime.now());

        return review;
    }

    @Transactional
    public Review updateAdvancedReview(UserEntity user, Bot bot, Long reviewId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = entityUtil.getReviewById(user, bot, reviewId);

        LOGGER.info("User " + review.getUser().getFullName()
                + " wants to update their review's content for course " + review.getCourse()
                .getId() + ". Current content id is " + review.getContent().getId() + ".");
        if (review.getContent() == null) {
            throw new ForbiddenOperationException("Unable to update review " + reviewId
                    + "'s content because it has never been submitted", localizationLoader
                    .localize(Error.UPDATE_CONTENT_NOT_PRESENT, user));
        }

        review.setContent(contentService.parseAndPersistContent(user, bot, messages));
        review.setLastUpdateTimestamp(LocalDateTime.now());

        return review;
    }

    @Transactional
    public Review addToUsersWhoRead(UserEntity user, Bot bot, Long reviewId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        
        final Review review = entityUtil.getReviewById(user, bot, reviewId);

        review.getMarkedAsReadBy().add(user);

        return review;
    }

    @Transactional(readOnly = true)
    public boolean doesReviewForUserAndCourseExist(UserEntity user, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return reviewRepository.existsByCourseIdAndUserId(courseId, courseId);
    }

    @Transactional(readOnly = true)
    public boolean doesAdvancedReviewForUserAndCourseExist(UserEntity user, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return reviewRepository.existsByCourseIdAndUserIdAndContentIsNotNull(courseId, courseId);
    }

    @Transactional(readOnly = true)
    public Page<Review> getReviewsForCourse(Long courseId, Pageable pageable) {
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(pageable, "pageable cannot be null");

        return reviewRepository.findByCourseId(courseId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Review> getNewReviewsForUser(UserEntity user, Bot bot, Pageable pageable) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(pageable, "pageable cannot be null");

        final Page<Review> reviews = reviewRepository.findNewReviewsForUser(user.getId(), bot.getId(), pageable);

        reviewRepository.findByIdIn(reviews.stream().map(r -> r.getId()).toList());

        return reviews;
    }

    @Transactional(readOnly = true)
    public Page<Review> getNewReviewsForUserAndCourse(UserEntity user, Long courseId, Pageable pageable) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(pageable, "pageable cannot be null");

        final Page<Review> reviews = reviewRepository.findNewReviewsForUserAndCourse(user.getId(), courseId, pageable);

        reviewRepository.findByIdIn(reviews.stream().map(r -> r.getId()).toList());

        return reviews;
    }

    @Transactional(readOnly = true)
    public List<Review> getArchiveReviewsForUser(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final List<Review> reviews = reviewRepository.findArchiveReviewsForUser(user.getId(), bot.getId());

        return reviews;
    }

    @Transactional(readOnly = true)
    public List<Review> getArchiveReviewsForUserAndCourse(UserEntity user, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final List<Review> reviews = reviewRepository.findArchiveReviewsForUserAndCourse(user.getId(), courseId);

        return reviews;
    }
}
