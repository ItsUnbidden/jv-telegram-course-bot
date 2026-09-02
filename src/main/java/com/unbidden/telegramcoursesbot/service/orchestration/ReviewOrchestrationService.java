package com.unbidden.telegramcoursesbot.service.orchestration;

import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.TextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto.Result;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.ArchiveReviewsException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.MenuTerminationGroupKey;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.repository.impl.InMemoryReviewSessionRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.model.ReviewSession;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(ReviewOrchestrationService.class);

    private static final String REVIEW_ID_PARAM = "reviewId";
    private static final String COURSE_ID_PARAM = "courseId";

    private static final String ARCHIVE_REVIEWS_FILE_NAME = "archive_reviews_user_%s_course_%s";
    private static final String ARCHIVE_REVIEWS_FILE_FORMAT = ".txt";
    private static final String TEMP_FILE_NAME = "reviews_for_%s";

    private final ArchiveReviewsDao archiveReviewsDao;

    private final InMemoryReviewSessionRepository reviewSessionRepository;

    private final ReviewService reviewService;

    private final MenuOrchestrationService menuService;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final TextUtil textUtil;

    private final EntityUtil entityUtil;

    @Value("${telegram.bot.reviews.page_size}")
    private Integer pageSize;

    public Review checkReviewForComment(BotRole botRole, Long reviewId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");

        return reviewService.checkReviewForComment(botRole, reviewId);
    }

    public void initiateBasicReview(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        if (reviewService.doesReviewForUserAndCourseExist(botRole.getUser().getId(), courseId)) {
            throw new ActionExpiredException("Unable to initiate a new review menu "
                    + "for user " + botRole.getUser().getId() + " since they have already left a review "
                    + "for course " + courseId, localizationLoader.localize(
                    Error.REVIEW_ALREADY_PRESENT, botRole));
        }

        LOGGER.info("Sending basic review menu for course " + courseId + " to user "
                + botRole.getUser().getFullName() + "...");
        menuService.initiateMenu(botRole, MenuKey.LEAVE_BASIC_REVIEW,
                COURSE_ID_PARAM, courseId.toString(),
                MenuTerminationGroupKey.LEAVE_BASIC_REVIEW, courseId);
    }

    public Review commitBasicReview(BotRole botRole, Long courseId, int courseGrade) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        if (reviewService.doesReviewForUserAndCourseExist(botRole.getUser().getId(), courseId)) {
            throw new ActionExpiredException("Unable to create a new review entity "
                    + "for user " + botRole.getUser().getId() + " since they have already left a review "
                    + "for course " + courseId, localizationLoader.localize(
                    Error.COMMIT_BASIC_REVIEW_FAILURE, botRole));
        }

        LOGGER.info("User " + botRole.getUser().getFullName() + " wants to submit a basic review for course "
                + courseId + ". Their course grade is " + courseGrade + ".");
        final Course course = entityUtil.getCourseById(botRole, courseId);
        final Review review = reviewService.createNewReview(botRole.getUser().getId(), courseId, courseGrade);

        LOGGER.debug("New review " + review.getId() + " has been created. Sending confirmation message...");
        final SendMessageResultDto confirmationMessage = clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.BASIC_REVIEW_SUBMITTED, botRole, new Localizations.Service.BasicReviewSubmittedParams(
                    contentService.getLocalizedText(botRole, course.getTitle().getId()))));
        LOGGER.debug("Message sent. An offer to provide an advanced review "
                + "will be sent. All 'leave basic review' menus will be terminated.");
        menuService.terminateMenuGroup(MenuTerminationGroupKey.LEAVE_BASIC_REVIEW, course.getId());

        if (confirmationMessage.getResult() == Result.OK) {
            LOGGER.info("Sending advanced review menu for course " + review.getCourse().getId()
                    + " to user " +  review.getUser().getId() + "...");
            menuService.initiateMenu(botRole, MenuKey.LEAVE_ADVANCED_REVIEW, COURSE_ID_PARAM, review.getCourse().getId().toString(),
                    confirmationMessage.getMessage().getMessageId(), MenuTerminationGroupKey.LEAVE_ADVANCED_REVIEW, review.getId());
        } else {
            LOGGER.error("Failed to send basic review confirmation message. Advanced review menu won't be sent.");
            // TODO: introduce fallback
        }

        return review;
    }

    public Review commitAdvancedReview(BotRole botRole, Long courseId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = reviewService.upgradeReview(botRole, courseId, messages);

        LOGGER.debug("Review " + review.getId() + " has been updated. Sending confirmation message...");

        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.ADVANCED_REVIEW_SUBMITTED, botRole,
                new Localizations.Service.AdvancedReviewSubmittedParams(contentService.getLocalizedText(
                    botRole, review.getCourse().getTitle().getId()))));
        LOGGER.info("Message sent. Review " + review.getId() + " has been updated to include advanced feedback. "
                + "All advanced review menus will be terminated.");
        menuService.terminateMenuGroup(MenuTerminationGroupKey.LEAVE_ADVANCED_REVIEW, review.getId());

        return review;
    }

    public Review leaveComment(BotRole botRole, Long reviewId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = reviewService.leaveComment(botRole, reviewId, messages);
        final BotRole targetRole = entityUtil.getActiveBotRole(botRole, review.getUser().getId());

        LOGGER.debug("Review " + reviewId + " has been updated. Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.COMMENT_SUBMITTED, botRole));
        LOGGER.debug("Message sent. Sending notification to the review's owner..."); 
        clientManager.sendMessage(targetRole, localizationLoader.localize(
                Localizations.Service.COMMENT_SUBMITTED_NOTIFICATION, targetRole,
                new Localizations.Service.CommentSubmittedNotificationParams(
                    contentService.getLocalizedText(targetRole, review.getCourse().getTitle().getId()),
                    botRole.getUser().getFullName(),
                    entityUtil.getLocalizedTitle(targetRole, botRole))));
        LOGGER.debug("Notificaton sent. Sending comment content...");
        contentService.sendContent(targetRole, review.getCommentContent().getId());
        
        LOGGER.debug("Content sent. Review " + reviewId + " will be removed from the active review session.");
        removeFromReviewSession(targetRole, review);

        return review;
    }

    public Review updateComment(BotRole botRole, Long reviewId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = reviewService.updateComment(botRole, reviewId, messages);
        final BotRole targetRole = entityUtil.getActiveBotRole(botRole, review.getUser().getId());

        LOGGER.debug("Review has been updated Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.COMMENT_SUBMITTED, botRole));
        LOGGER.debug("Message sent. Sending notification to the review's owner...");
        clientManager.sendMessage(targetRole, localizationLoader.localize(Localizations.Service.COMMENT_SUBMITTED_NOTIFICATION,
                targetRole, new Localizations.Service.CommentSubmittedNotificationParams(
                    contentService.getLocalizedText(targetRole, review.getCourse().getTitle().getId()),
                    botRole.getUser().getFullName(),
                    entityUtil.getLocalizedTitle(targetRole, botRole))));
        LOGGER.debug("Notificaton sent. Sending comment content...");
        contentService.sendContent(targetRole, review.getCommentContent().getId());
        LOGGER.debug("New comment content has been sent.");

        return review;
    }

    public Review updateCourseGrade(BotRole botRole, Long courseId, int newGrade) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.state(newGrade > 0 && newGrade <= 10, "newGrade must be an int between 1 and 10");
        
        final Review review = reviewService.updateCourseGrade(botRole, courseId, newGrade);

        LOGGER.debug("Review has been updated. Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.REVIEW_COURSE_GRADE_UPDATED,
                botRole, new Localizations.Service.ReviewCourseGradeUpdatedParams(
                    contentService.getLocalizedText(botRole, review.getCourse().getTitle().getId()))));
        LOGGER.debug("Message sent."); 

        return review;
    }

    public Review updateAdvancedReview(BotRole botRole, Long courseId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final Review review = reviewService.updateAdvancedReview(botRole, courseId, messages);

        LOGGER.debug("Review object recompiled. Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.REVIEW_CONTENT_UPDATED,
                botRole, new Localizations.Service.ReviewContentUpdatedParams(
                    contentService.getLocalizedText(botRole, review.getCourse().getTitle().getId()))));
        LOGGER.debug("Message sent.");

        return review;
    }

    public Review markReviewAsRead(BotRole botRole, Long reviewId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");

        final Review review = reviewService.addToUsersWhoRead(botRole, reviewId);

        removeFromReviewSession(botRole, review);
        return review;
    }

    public void sendNewReviewsForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        final List<Review> reviews = reviewService.getNewReviewsForUser(
                botRole, PageRequest.of(0, pageSize)).stream().toList();

        LOGGER.info("Sending " + reviews.size() + " new review(s) to user "
                + botRole.getUser().getFullName());
        sendReviews(botRole, reviews);
        LOGGER.debug("Reviews sent. Updating review session...");

        reviewSessionRepository.save(new ReviewSession(botRole.getId(), reviews.size(), null));
        LOGGER.debug("Review session updated.");
    }

    public void sendNewReviewsForUserAndCourse(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final List<Review> reviews = reviewService.getNewReviewsForUserAndCourse(botRole.getUser().getId(),
                courseId, PageRequest.of(0, pageSize)).stream().toList();

        LOGGER.info("Sending " + reviews.size() + " new review(s) for course " + courseId
                + " to user " + botRole.getUser().getFullName());
        sendReviews(botRole, reviews);
        LOGGER.debug("Reviews sent. Updating review session...");

        reviewSessionRepository.save(new ReviewSession(botRole.getId(), reviews.size(), courseId));
        LOGGER.debug("Review session updated.");
    }

    public void sendArchiveReviewsForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        final List<Review> archiveReviews = reviewService.getArchiveReviewsForUser(botRole);
        
        LOGGER.info("Sending " + archiveReviews.size() + " archive review(s) to user "
                + botRole.getUser().getFullName());
        sendArchiveReviews(botRole, archiveReviews, null);
        LOGGER.debug("Reviews sent.");
    }

    public void sendArchiveReviewsForUserAndCourse(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final List<Review> archiveReviews = reviewService.getArchiveReviewsForUserAndCourse(
                botRole.getUser().getId(), courseId);
        
        LOGGER.info("Sending " + archiveReviews.size() + " archive review(s) for course "
                + courseId + " to user " + botRole.getUser().getFullName());
        sendArchiveReviews(botRole, archiveReviews, courseId);
        LOGGER.debug("Reviews sent.");
    }

    private void removeFromReviewSession(BotRole botRole, Review review) {
        final Optional<ReviewSession> currentReviewSessionOpt = reviewSessionRepository.find(botRole.getId());

        if (currentReviewSessionOpt.isPresent()) {
            final var currentReviewSession = currentReviewSessionOpt.get();

            currentReviewSession.setCounter(currentReviewSession.getCounter() - 1);
    
            menuService.terminateMenuGroup(MenuTerminationGroupKey.REVIEW_ACTIONS, review.getId());
    
            if (currentReviewSession.getCounter() < 1) {
                if (currentReviewSession.getCourseId() != null) {
                    sendNewReviewsForUserAndCourse(botRole, currentReviewSession.getCourseId());
                } else {
                    sendNewReviewsForUser(botRole);
                }
            }
        }
    }

    private void sendReviews(BotRole botRole, List<Review> reviews) {
        if (reviews.isEmpty()) {
            LOGGER.info("No further reviews are availbable for user " + botRole.getUser().getId() + ".");
            clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.NO_NEW_REVIEWS_FOR_USER, botRole));
            reviewSessionRepository.remove(botRole.getId());
            return;
        }

        for (final Review review : reviews) {
            LOGGER.info("Compiling review info for review " + review.getId() + "...");
            final String localizedCourseName = contentService.getLocalizedText(botRole, review.getCourse().getTitle().getId());
            final String notAvailable = localizationLoader.localize(Localizations.Service.NOT_AVAILABLE, botRole).getData();

            final SendMessageResultDto message;
            if (review.getContent() != null && review.getCommentContent() != null) {
                LOGGER.debug("Review is advanced and has a comment.");

                clientManager.sendMessage(botRole, localizationLoader.localize(
                        Localizations.Service.REVIEW_INFO_CONTENT_COMMENT, botRole, new Localizations.Service.ReviewInfoContentCommentParams(
                            review.getUser().getFullName(),
                            review.getBasicSubmittedTimestamp(),
                            review.getLastUpdateTimestamp() != null ? review.getLastUpdateTimestamp().toString() : notAvailable,
                            localizedCourseName,
                            review.getCourseGrade(), 
                            review.getUsersWhoReadAsString(),
                            review.getCommentedBy().getFullName(),
                            review.getCommentedAt(),
                            review.getContent().getId(), 
                            review.getAdvancedSubmittedTimestamp())));

                final List<SendMessageResultDto> sentMessages = contentService.sendContent(botRole, review.getContent().getId());

                message = getMenuMessage(botRole, sentMessages);
            } else if (review.getContent() != null) {
                LOGGER.debug("Review is advanced.");

                clientManager.sendMessage(botRole, localizationLoader.localize(
                        Localizations.Service.REVIEW_INFO_CONTENT, botRole, new Localizations.Service.ReviewInfoContentParams(
                            review.getUser().getFullName(),
                            review.getBasicSubmittedTimestamp(),
                            review.getLastUpdateTimestamp() != null ? review.getLastUpdateTimestamp().toString() : notAvailable,
                            localizedCourseName,
                            review.getCourseGrade(),
                            review.getUsersWhoReadAsString(),
                            review.getContent().getId(),
                            review.getAdvancedSubmittedTimestamp())));

                final List<SendMessageResultDto> sentMessages = contentService.sendContent(botRole, review.getContent().getId());

                message = getMenuMessage(botRole, sentMessages);
            } else if (review.getCommentContent() != null) {
                LOGGER.debug("Review has a comment.");
                
                message = clientManager.sendMessage(botRole, localizationLoader.localize(
                        Localizations.Service.REVIEW_INFO_COMMENT, botRole, new Localizations.Service.ReviewInfoCommentParams(
                            review.getUser().getFullName(),
                            review.getBasicSubmittedTimestamp(),
                            review.getLastUpdateTimestamp() != null ? review.getLastUpdateTimestamp().toString() : notAvailable,
                            localizedCourseName,
                            review.getCourseGrade(),
                            review.getUsersWhoReadAsString(),
                            review.getCommentedBy().getFullName(),
                            review.getCommentedAt())));
            } else {
                LOGGER.debug("Review is basic with no comment.");

                message = clientManager.sendMessage(botRole, localizationLoader.localize(
                        Localizations.Service.REVIEW_INFO, botRole, new Localizations.Service.ReviewInfoParams(
                            review.getUser().getFullName(),
                            review.getBasicSubmittedTimestamp(),
                            review.getLastUpdateTimestamp() != null ? review.getLastUpdateTimestamp().toString() : notAvailable,
                            localizedCourseName,
                            review.getCourseGrade(),
                            review.getUsersWhoReadAsString())));
            }
            if (message.getResult() == Result.OK) {
                menuService.initiateMenu(botRole, MenuKey.REVIEW_ACTIONS, REVIEW_ID_PARAM, review.getId().toString(),
                        message.getMessage().getMessageId(), MenuTerminationGroupKey.REVIEW_ACTIONS, review.getId());
            } else {
                LOGGER.error("Failed to send the review message. Review actions menu will not be attached.");
                // TODO: introduce fallback
            }
        }
    }

    private void sendArchiveReviews(BotRole botRole, List<Review> reviews, Long courseId) {
        if (reviews.size() == 0) {
            throw new ArchiveReviewsException("No archive reviews available",
                    localizationLoader.localize(Error.NO_ARCHIVE_REVIEWS_AVAILABLE, botRole));
        }
        final Path tempFile = archiveReviewsDao.createTempFile(TEMP_FILE_NAME.formatted(botRole.getUser().getId()));
        final StringBuilder builder = new StringBuilder();

        for (final Review review : reviews) {
            final String reviewInfo = textUtil.getArchiveReviewInfo(review, contentService.getLocalizedText(
                    botRole, review.getCourse().getTitle().getId()), builder);

            LOGGER.debug("Writing review " + review.getId() + " to a temp file "
                    + tempFile + "...");
            archiveReviewsDao.write(tempFile, reviewInfo);
            LOGGER.debug("Review has been saved to the temp file.");
            builder.delete(0, builder.length());
        }
        final String fileName = ARCHIVE_REVIEWS_FILE_NAME.formatted(botRole.getUser().getId(),
                (courseId == null) ? "all" : courseId) + ARCHIVE_REVIEWS_FILE_FORMAT;

        try {
            LOGGER.debug("Reading temp file " + tempFile + " and sending reviews file to user "
                    + botRole.getUser().getId() + "...");
            final InputStream inputStream = archiveReviewsDao.read(tempFile);

            try {
                clientManager.getClient(botRole.getBot()).execute(SendDocument.builder()
                        .chatId(botRole.getUser().getId())
                        .document(new InputFile(inputStream, fileName))
                        .build());
                LOGGER.info("Archive reviews file has been sent to user " + botRole.getUser().getId() + ".");
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to send file " + fileName + " to user "
                        + botRole.getUser().getId(), localizationLoader.localize(
                        Error.SEND_FILE_FAILURE, botRole), e);
            } finally {
                inputStream.close();
                LOGGER.debug("Input stream has been closed.");
            }
        } catch (IOException e) {
            throw new ArchiveReviewsException("Unable to close the stream after the temp file "
                    + tempFile + " has been read for user " + botRole.getUser().getId(), null, e);
        }
    }

    private SendMessageResultDto getMenuMessage(BotRole botRole, List<SendMessageResultDto> sentMessages) {
        final SendMessageResultDto menuMessage;

        if (sentMessages.size() > 1) {
            LOGGER.debug("Review content is a media group. To avoid Telegram restrictions, an "
                    + "additional message will be sent to user " + botRole.getUser().getId()
                    + " to attach the feedback menu to.");

            menuMessage = clientManager.sendMessage(botRole, localizationLoader
                    .localize(Localizations.Service.REVIEW_MEDIA_GROUP_BYPASS, botRole));
            LOGGER.debug("Additional message for menu has been sent.");
        } else {
            LOGGER.debug("Review content is not a media group. Menu will be attached to it.");    
            menuMessage = sentMessages.get(0);
        }

        return menuMessage;
    }
}
