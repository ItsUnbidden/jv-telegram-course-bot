package com.unbidden.telegramcoursesbot.service.orchestration;

import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.TextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dao.ArchiveReviewsDao;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.ArchiveReviewsException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.MenuTerminationGroupKey;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(ReviewOrchestrationService.class);

    private static final String REVIEW_ID_PARAM = "reviewId";
    private static final String COURSE_ID_PARAM = "courseId";

    private static final Map<Long, ReviewSession> REVIEW_SESSION_MAP = new HashMap<>();

    private static final String ARCHIVE_REVIEWS_FILE_NAME = "archive_reviews_user_%s_course_%s";
    private static final String ARCHIVE_REVIEWS_FILE_FORMAT = ".txt";
    private static final String TEMP_FILE_NAME = "reviews_for_%s";

    private final ArchiveReviewsDao archiveReviewsDao;

    private final ReviewService reviewService;

    private final MenuOrchestrationService menuService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final TextUtil textUtil;

    private final EntityUtil entityUtil;

    @Value("${telegram.bot.reviews.page_size}")
    private Integer pageSize;

    public void initiateBasicReview(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        if (reviewService.doesReviewForUserAndCourseExist(user, courseId)) {
            throw new ActionExpiredException("Unable to initiate a new review menu "
                    + "for user " + user.getId() + " since they have already left a review "
                    + "for course " + courseId, localizationLoader.localize(
                    Error.REVIEW_ALREADY_PRESENT, user));
        }

        LOGGER.info("Sending basic review menu for course " + courseId + " to user "
                + user.getFullName() + "...");
        menuService.initiateMenu(user, bot, MenuKey.LEAVE_BASIC_REVIEW,
                COURSE_ID_PARAM, courseId.toString(),
                MenuTerminationGroupKey.LEAVE_BASIC_REVIEW, courseId);
    }

    public Review commitBasicReview(UserEntity user, Bot bot, Long courseId,
            int courseGrade) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        if (reviewService.doesReviewForUserAndCourseExist(user, courseId)) {
            throw new ActionExpiredException("Unable to create a new review entity "
                    + "for user " + user.getId() + " since they have already left a review "
                    + "for course " + courseId, localizationLoader.localize(
                    Error.COMMIT_BASIC_REVIEW_FAILURE, user));
        }

        LOGGER.info("User " + user.getFullName() + " wants to submit a basic review for course "
                + courseId + ". Their course grade is " + courseGrade + ".");
        final Course course = entityUtil.getCourseById(user, bot, courseId);
        final Review review = reviewService.createNewReview(user, courseId, courseGrade);

        LOGGER.debug("New review " + review.getId() + " has been created. Sending confirmation message...");
        final Message confirmationMessage = clientManager.getClient(course.getBot())
                .sendMessage(user, localizationLoader.localize(
                Localizations.Service.BASIC_REVIEW_SUBMITTED, user,
                new Localizations.Service.BasicReviewSubmittedParams(
                    contentService.getLocalizedText(user, bot, course.getTitle().getId()))));
        LOGGER.debug("Message sent. An offer to provide an advanced review "
                + "will be sent. All 'leave basic review' menus will be terminated.");
        menuService.terminateMenuGroup(MenuTerminationGroupKey.LEAVE_BASIC_REVIEW, course.getId());

        initiateAdvancedReview(review, bot, confirmationMessage.getMessageId());

        return review;
    }

    public Review commitAdvancedReview(UserEntity user, Bot bot, Long reviewId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = reviewService.upgradeReview(user, bot, reviewId, messages);

        LOGGER.debug("Review " + reviewId + " has been updated. Sending confirmation message...");

        clientManager.getClient(bot).sendMessage(review.getUser(), localizationLoader.localize(
                Localizations.Service.ADVANCED_REVIEW_SUBMITTED, review.getUser(),
                new Localizations.Service.AdvancedReviewSubmittedParams(contentService.getLocalizedText(user,
                    bot, review.getCourse().getTitle().getId()))));
        LOGGER.info("Message sent. Review " + reviewId + " has been updated to include advanced feedback. "
                + "All advanced review menus will be terminated.");
        menuService.terminateMenuGroup(MenuTerminationGroupKey.LEAVE_ADVANCED_REVIEW, review.getId());

        return review;
    }

    public Review leaveComment(UserEntity user, Bot bot, Long reviewId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = reviewService.leaveComment(user, bot, reviewId, messages);

        LOGGER.debug("Review " + reviewId + " has been updated. Sending confirmation message...");
        clientManager.getClient(review.getCourse().getBot()).sendMessage(user,
                localizationLoader.localize(Localizations.Service.COMMENT_SUBMITTED, user));
        LOGGER.debug("Message sent. Sending notification to the review's owner..."); 
        clientManager.getClient(review.getCourse().getBot())
                .sendMessage(review.getUser(), localizationLoader.localize(
                Localizations.Service.COMMENT_SUBMITTED_NOTIFICATION, user,
                new Localizations.Service.CommentSubmittedNotificationParams(
                    contentService.getLocalizedText(user, bot, review.getCourse().getTitle().getId()), user.getFullName(),
                    entityUtil.getLocalizedTitle(review.getUser(), review.getCourse().getBot(), user))));
        LOGGER.debug("Notificaton sent. Sending comment content...");
        contentService.sendContent(review.getUser(), bot, review.getCommentContent().getId());
        
        LOGGER.debug("Content sent. Review " + reviewId + " will be removed from the active review session.");
        removeFromReviewSession(user, bot, review);

        return review;
    }

    public Review updateComment(UserEntity user, Bot bot, Long reviewId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = reviewService.updateComment(user, bot, reviewId, messages);

        LOGGER.debug("Review has been updated Sending confirmation message...");
        clientManager.getClient(review.getCourse().getBot()).sendMessage(user,
                localizationLoader.localize(Localizations.Service.COMMENT_SUBMITTED, user));
        LOGGER.debug("Message sent. Sending notification to the review's owner...");
        clientManager.getClient(review.getCourse().getBot()).sendMessage(review.getUser(),
                localizationLoader.localize(Localizations.Service.COMMENT_SUBMITTED_NOTIFICATION,
                user, new Localizations.Service.CommentSubmittedNotificationParams(
                    contentService.getLocalizedText(user, bot, review.getCourse().getTitle().getId()), user.getFullName(),
                    entityUtil.getLocalizedTitle(review.getUser(), review.getCourse().getBot(), user))));
        LOGGER.debug("Notificaton sent. Sending comment content...");
        contentService.sendContent(review.getUser(), review.getCourse().getBot(),
                review.getCommentContent().getId());
        LOGGER.debug("New comment content has been sent.");

        return review;
    }

    public Review updateCourseGrade(UserEntity user, Bot bot, Long reviewId, int newGrade) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.state(newGrade > 0 && newGrade <= 10, "newGrade must be an int between 1 and 10");
        
        final Review review = reviewService.updateCourseGrade(user, bot, reviewId, newGrade);

        LOGGER.debug("Review has been updated. Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(review.getUser(), localizationLoader.localize(
                Localizations.Service.REVIEW_COURSE_GRADE_UPDATED, review.getUser(),
                new Localizations.Service.ReviewCourseGradeUpdatedParams(
                    contentService.getLocalizedText(user, bot, review.getCourse().getTitle().getId()))));
        LOGGER.debug("Message sent."); 

        return review;
    }

    public Review updateAdvancedReview(UserEntity user, Bot bot, Long reviewId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = reviewService.updateAdvancedReview(user, bot, reviewId, messages);

        LOGGER.debug("Review object recompiled. Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.REVIEW_CONTENT_UPDATED, review.getUser(),
                new Localizations.Service.ReviewContentUpdatedParams(
                    contentService.getLocalizedText(user, bot, review.getCourse().getTitle().getId()))));
        LOGGER.debug("Message sent.");

        return review;
    }

    public Review markReviewAsRead(UserEntity user, Bot bot, Long reviewId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");

        final Review review = reviewService.addToUsersWhoRead(user, bot, reviewId);

        removeFromReviewSession(user, bot, review);
        return review;
    }

    public void sendNewReviewsForUser(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final List<Review> reviews = reviewService.getNewReviewsForUser(
                user, bot, PageRequest.of(0, pageSize)).stream().toList();

        LOGGER.info("Sending " + reviews.size() + " new review(s) to user "
                + user.getFullName());
        sendReviews(user, bot, reviews);
        LOGGER.debug("Reviews sent. Updating review session...");

        updateReviewSession(user, reviews.size(), null);
        LOGGER.debug("Review session updated.");
    }

    public void sendNewReviewsForUserAndCourse(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final List<Review> reviews = reviewService.getNewReviewsForUserAndCourse(user,
                courseId, PageRequest.of(0, pageSize)).stream().toList();

        LOGGER.info("Sending " + reviews.size() + " new review(s) for course " + courseId
                + " to user " + user.getFullName());
        sendReviews(user, bot, reviews);
        LOGGER.debug("Reviews sent. Updating review session...");

        updateReviewSession(user, reviews.size(), courseId);
        LOGGER.debug("Review session updated.");
    }

    public void sendArchiveReviewsForUser(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final List<Review> archiveReviews = reviewService.getArchiveReviewsForUser(user, bot);
        
        LOGGER.info("Sending " + archiveReviews.size() + " archive review(s) to user "
                + user.getFullName());
        sendArchiveReviews(archiveReviews, user, null);
        LOGGER.debug("Reviews sent.");
    }

    public void sendArchiveReviewsForUserAndCourse(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final List<Review> archiveReviews = reviewService.getArchiveReviewsForUserAndCourse(user, courseId);
        
        LOGGER.info("Sending " + archiveReviews.size() + " archive review(s) for course "
                + courseId + " to user " + user.getFullName());
        sendArchiveReviews(archiveReviews, user, courseId);
        LOGGER.debug("Reviews sent.");
    }

    private void removeFromReviewSession(UserEntity user, Bot bot, Review review) {
        final ReviewSession currentReviewSession = REVIEW_SESSION_MAP.get(user.getId());

        currentReviewSession.counter--;

        menuService.terminateMenuGroup(MenuTerminationGroupKey.REVIEW_ACTIONS, review.getId());

        if (currentReviewSession.counter < 1) {
            if (currentReviewSession.courseId != null) {
                sendNewReviewsForUserAndCourse(user, bot, currentReviewSession.courseId);
            } else {
                sendNewReviewsForUser(user, bot);
            }
        }
    }

    private void initiateAdvancedReview(Review review, Bot bot, Integer messageId) {
        LOGGER.info("Sending advanced review menu for course " + review.getCourse().getId()
                + " to user " +  review.getUser().getId() + "...");
        menuService.initiateMenu(review.getUser(), bot, MenuKey.LEAVE_ADVANCED_REVIEW,
                REVIEW_ID_PARAM, review.getCourse().getId().toString(), messageId, MenuTerminationGroupKey.LEAVE_ADVANCED_REVIEW, review.getId());
    }

    private void sendReviews(UserEntity user, Bot bot, List<Review> reviews) {
        if (reviews.isEmpty()) {
            LOGGER.info("No further reviews are availbable for user " + user.getId() + ".");
            clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                    Localizations.Service.NO_NEW_REVIEWS_FOR_USER, user));
            return;
        }

        for (final Review review : reviews) {
            LOGGER.info("Compiling review info for review " + review.getId() + "...");
            final String localizedCourseName = contentService.getLocalizedText(user, bot,
                    review.getCourse().getTitle().getId());

            final Message message;
            if (review.getContent() != null && review.getCommentContent() != null) {
                LOGGER.debug("Review is advanced and has a comment.");

                clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                        Localizations.Service.REVIEW_INFO_CONTENT_COMMENT, user, new Localizations.Service.ReviewInfoContentCommentParams(
                            review.getUser().getFullName(), review.getBasicSubmittedTimestamp(), review.getLastUpdateTimestamp(),
                            localizedCourseName, review.getCourseGrade(), review.getUsersWhoReadAsString(),
                            review.getCommentedBy().getFullName(), review.getCommentedAt(), review.getContent().getId(), 
                            review.getAdvancedSubmittedTimestamp())));

                final List<Message> sentMessages = contentService.sendContent(user, bot, review.getContent().getId());

                message = getMenuMessage(user, bot, sentMessages);
            } else if (review.getContent() != null) {
                LOGGER.debug("Review is advanced.");

                clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                        Localizations.Service.REVIEW_INFO_CONTENT, user, new Localizations.Service.ReviewInfoContentParams(
                            review.getUser().getFullName(), review.getBasicSubmittedTimestamp(), review.getLastUpdateTimestamp(),
                            localizedCourseName, review.getCourseGrade(), review.getUsersWhoReadAsString(),
                            review.getContent().getId(), review.getAdvancedSubmittedTimestamp())));

                final List<Message> sentMessages = contentService.sendContent(user, bot, review.getContent().getId());

                message = getMenuMessage(user, bot, sentMessages);
            } else if (review.getCommentContent() != null) {
                LOGGER.debug("Review has a comment.");

                message = clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                        Localizations.Service.REVIEW_INFO_COMMENT, user, new Localizations.Service.ReviewInfoCommentParams(
                            review.getUser().getFullName(), review.getBasicSubmittedTimestamp(), review.getLastUpdateTimestamp(),
                            localizedCourseName, review.getCourseGrade(), review.getUsersWhoReadAsString(),
                            review.getCommentedBy().getFullName(), review.getCommentedAt())));
            } else {
                LOGGER.debug("Review is basic with no comment.");

                message = clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                        Localizations.Service.REVIEW_INFO, user, new Localizations.Service.ReviewInfoParams(
                            review.getUser().getFullName(), review.getBasicSubmittedTimestamp(), review.getLastUpdateTimestamp(),
                            localizedCourseName, review.getCourseGrade(), review.getUsersWhoReadAsString())));
            }
            menuService.initiateMenu(user, bot, MenuKey.REVIEW_ACTIONS, REVIEW_ID_PARAM, review.getId().toString(), message.getMessageId(),
                    MenuTerminationGroupKey.REVIEW_ACTIONS, review.getId());
        }
    }

    private void sendArchiveReviews(List<Review> reviews, UserEntity user, Long courseId) {
        if (reviews.size() == 0) {
            throw new ArchiveReviewsException("No archive reviews available", localizationLoader
                    .localize(Error.NO_ARCHIVE_REVIEWS_AVAILABLE, user));
        }
        final Bot bot = reviews.get(0).getCourse().getBot();
        final Path tempFile = archiveReviewsDao.createTempFile(TEMP_FILE_NAME.formatted(
                user.getId()));
        final StringBuilder builder = new StringBuilder();

        for (final Review review : reviews) {
            final String reviewInfo = textUtil.getArchiveReviewInfo(review, contentService.getLocalizedText(
                    user, bot, review.getCourse().getTitle().getId()), builder);

            LOGGER.debug("Writing review " + review.getId() + " to a temp file "
                    + tempFile + "...");
            archiveReviewsDao.write(tempFile, reviewInfo);
            LOGGER.debug("Review has been saved to the temp file.");
            builder.delete(0, builder.length());
        }
        final String fileName = ARCHIVE_REVIEWS_FILE_NAME.formatted(user.getId(),
                (user.getId() == null) ? "all" : courseId) + ARCHIVE_REVIEWS_FILE_FORMAT;

        try {
            LOGGER.debug("Reading temp file " + tempFile + " and sending reviews file to user "
                    + user.getId() + "...");
            final InputStream inputStream = archiveReviewsDao.read(tempFile);

            try {
                clientManager.getClient(bot).execute(SendDocument.builder()
                        .chatId(user.getId())
                        .document(new InputFile(inputStream, fileName))
                        .build());
                LOGGER.info("Archive reviews file has been sent to user " + user.getId() + ".");
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to send file " + fileName + " to user "
                        + user.getId(), localizationLoader.localize(
                        Error.SEND_FILE_FAILURE, user), e);
            } finally {
                inputStream.close();
                LOGGER.debug("Input stream has been closed.");
            }
        } catch (IOException e) {
            throw new ArchiveReviewsException("Unable to close the stream after the temp file "
                    + tempFile + " has been read for user " + user.getId(), null, e);
        }
    }

    private void updateReviewSession(UserEntity user, int numberOfReviews, Long courseId) {
        final ReviewSession reviewSession = REVIEW_SESSION_MAP.get(user.getId());
        
        courseId = (courseId != null && numberOfReviews > 0) ? courseId : null;
        if (reviewSession != null) {
            reviewSession.counter = numberOfReviews;
            reviewSession.courseId = courseId;
        } else {
            REVIEW_SESSION_MAP.put(user.getId(), new ReviewSession(courseId, numberOfReviews));
        }
    }

    private Message getMenuMessage(UserEntity user, Bot bot, List<Message> sentMessages) {
        final Message menuMessage;

        if (sentMessages.size() > 1) {
            LOGGER.debug("Review content is a media group. To avoid Telegram restrictions, an "
                    + "additional message will be sent to user " + user.getId()
                    + " to attach the feedback menu to.");

            menuMessage = clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .localize(Localizations.Service.REVIEW_MEDIA_GROUP_BYPASS, user));
            LOGGER.debug("Additional message for menu has been sent.");
        } else {
            LOGGER.debug("Review content is not a media group. Menu will be attached to it.");    
            menuMessage = sentMessages.get(0);
        }

        return menuMessage;
    }

    private class ReviewSession {
        Long courseId;

        int counter;

        ReviewSession(Long courseId, int counter) {
            this.counter = counter;
            this.courseId = courseId;
        }
    }
}
