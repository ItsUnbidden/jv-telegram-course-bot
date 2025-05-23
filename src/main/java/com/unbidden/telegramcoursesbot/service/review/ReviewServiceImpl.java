package com.unbidden.telegramcoursesbot.service.review;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.dao.ArchiveReviewsDao;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.ArchiveReviewsException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.repository.ReviewRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private static final String REVIEW_ACTIONS_MENU = "m_rwA";
    private static final String LEAVE_ADVANCED_REVIEW_MENU = "m_laR";
    private static final String LEAVE_BASIC_REVIEW_MENU = "m_lbR";

    private static final String ARCHIVE_REVIEWS_FILE_NAME = "archive_reviews_user_%s_course_%s";
    private static final String ARCHIVE_REVIEWS_FILE_FORMAT = ".txt";
    private static final String TEMP_FILE_NAME = "reviews_for_%s";

    private static final String PARAM_COURSE_NAME = "${courseName}";
    private static final String PARAM_COMMENTER_FULL_NAME = "${commenterFullName}";
    private static final String PARAM_REVIEW_ID = "${reviewId}";

    private static final String SERVICE_REVIEW_INFO = "service_review_info";
    private static final String SERVICE_REVIEW_INFO_COMMENT = "service_review_info_comment";
    private static final String SERVICE_REVIEW_INFO_CONTENT = "service_review_info_content";
    private static final String SERVICE_REVIEW_INFO_CONTENT_COMMENT =
            "service_review_info_content_comment";
    private static final String SERVICE_NO_NEW_REVIEWS_FOR_USER =
            "service_no_new_reviews_for_user";
    private static final String SERVICE_REVIEW_COURSE_CONTENT_UPDATED =
            "service_review_course_content_updated";
    private static final String SERVICE_REVIEW_PLATFORM_GRADE_UPDATED =
            "service_review_platform_grade_updated";
    private static final String SERVICE_REVIEW_COURSE_GRADE_UPDATED =
            "service_review_course_grade_updated";
    private static final String SERVICE_COMMENT_SUBMITTED_NOTIFICATION =
            "service_comment_submitted_notification";
    private static final String SERVICE_COMMENT_SUBMITTED = "service_comment_submitted";
    private static final String SERVICE_ADVANCED_REVIEW_SUBMITTED =
            "service_advanced_review_submitted";
    private static final String SERVICE_BASIC_REVIEW_SUBMITTED = "service_basic_review_submitted";
    private static final String SERVICE_ADVANCED_REVIEW_TERMINAL =
            "service_advanced_review_terminal";
    private static final String SERVICE_BASIC_REVIEW_TERMINAL = "service_basic_review_terminal";
    private static final String SERVICE_REVIEW_MEDIA_GROUP_BYPASS =
            "service_review_media_group_bypass";

    private static final String ERROR_SEND_FILE_FAILURE = "error_send_file_failure";
    private static final String ERROR_LEAVE_COMMENT_FAILURE = "error_leave_comment_failure";
    private static final String ERROR_COMMIT_ADVANCED_REVIEW_FAILURE =
            "error_commit_advanced_review_failure";
    private static final String ERROR_COMMIT_BASIC_REVIEW_FAILURE =
            "error_commit_basic_review_failure";
    private static final String ERROR_ADVANCED_REVIEW_ALREADY_PRESENT =
            "error_advanced_review_already_present";
    private static final String ERROR_REVIEW_ALREADY_PRESENT = "error_review_already_present";
    private static final String ERROR_UPDATE_CONTENT_NOT_PRESENT =
            "error_update_content_not_present";
    private static final String ERROR_REVIEW_NOT_FOUND = "error_review_not_found";

    public static final String REVIEW_ACTIONS_MENU_TERMINATION = "review_%s_actions";
    private static final String SEND_BASIC_REVIEW_TERMINATION = "course_%s_send_basic_review";
    private static final String SEND_ADVANCED_REVIEW_TERMINATION =
            "review_%s_send_advanced_review";

    private static final Logger LOGGER = LogManager.getLogger(ReviewServiceImpl.class);

    private static final Map<Long, ReviewSession> CURRENT_REVIEW_COUNTER = new HashMap<>();

    private final ReviewRepository reviewRepository;

    private final MenuService menuService;

    private final ContentService contentService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final ArchiveReviewsDao archiveReviewsDao;

    private final TextUtil textUtil;

    private final CustomTelegramClient client;

    @Value("${telegram.bot.reviews.page_size}")
    private Integer pageSize;

    @Override
    public void initiateBasicReview(@NonNull UserEntity user, @NonNull Course course) {
        final Optional<Review> reviewOpt = reviewRepository.findByCourseNameAndUserId(
                course.getName(), user.getId());
        if (reviewOpt.isPresent()) {
            throw new ActionExpiredException("Unable to initiate a new review menu "
                    + "for user " + user.getId() + " since they already have left a review "
                    + "for course " + course.getName(), localizationLoader.getLocalizationForUser(
                    ERROR_REVIEW_ALREADY_PRESENT, user));
        }
        LOGGER.info("Sending basic review menu for course " + course.getName() + " to user "
                + user.getId() + "...");
        final Message menuMessage = menuService.initiateMenu(LEAVE_BASIC_REVIEW_MENU, user,
                course.getId().toString());
        LOGGER.info("Menu sent. Adding menu message " + menuMessage.getMessageId()
                + " to an MTG...");
        menuService.addToMenuTerminationGroup(user, user, menuMessage.getMessageId(),
                SEND_BASIC_REVIEW_TERMINATION.formatted(course.getId()),
                SERVICE_BASIC_REVIEW_TERMINAL);
        LOGGER.info("Message added to the MTG.");
    }

    @Override
    public void initiateAdvancedReview(@NonNull Review review, @NonNull Integer messageId) {
        if (review.getContent() != null) {
            throw new ActionExpiredException("Unable to initiate a new advanced review"
                    + " menu " + review.getId() + " because this review already has "
                    + "some content.", localizationLoader.getLocalizationForUser(
                    ERROR_ADVANCED_REVIEW_ALREADY_PRESENT, review.getUser()));
        }
        LOGGER.info("Sending advanced review menu for course " + review.getCourse().getName()
                + " to user " +  review.getUser().getId() + "...");
        menuService.initiateMenu(LEAVE_ADVANCED_REVIEW_MENU, review.getUser(),
                review.getCourse().getName(), messageId);
        LOGGER.info("Menu sent. Adding message " + messageId + " to an MTG...");
        menuService.addToMenuTerminationGroup(review.getUser(), review.getUser(), messageId,
                SEND_ADVANCED_REVIEW_TERMINATION.formatted(review.getId()), 
                SERVICE_ADVANCED_REVIEW_TERMINAL);
        LOGGER.info("Message added to the MTG.");
    }

    @Override
    @NonNull
    public Review commitBasicReview(@NonNull UserEntity user, @NonNull Course course,
            int courseGrade, int platformGrade) {
        final Optional<Review> reviewOpt = reviewRepository.findByCourseNameAndUserId(
                course.getName(), user.getId());
        if (reviewOpt.isPresent()) {
            throw new ActionExpiredException("Unable to create a new review entity "
                    + "for user " + user.getId() + " since they have already left a review "
                    + "for course " + course.getName(), localizationLoader.getLocalizationForUser(
                    ERROR_COMMIT_BASIC_REVIEW_FAILURE, user));
        }

        LOGGER.info("User " + user.getId() + " wants to submit a basic review for course "
                + course.getName() + ". Their course grade is " + courseGrade
                + " and platform grade is " + platformGrade + ".");
        final Review review = new Review();
        review.setUser(user);
        review.setCourse(course);
        review.setBasicSubmittedTimestamp(LocalDateTime.now());
        review.setOriginalCourseGrade(courseGrade);
        review.setCourseGrade(courseGrade);
        review.setOriginalPlatformGrade(platformGrade);
        review.setPlatformGrade(platformGrade);
        review.setMarkedAsReadBy(new ArrayList<>());

        LOGGER.info("Review object compiled. Sending confirmation message...");
        final Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_BASIC_REVIEW_SUBMITTED, user, PARAM_COURSE_NAME, course.getName());
        final Message confirmationMessage = client.sendMessage(user, localization);
        LOGGER.info("Message sent. Persisting review to the db...");
        reviewRepository.save(review);
        LOGGER.info("Persisted successfuly. An offer to provide an advanced review "
                + "will be sent. Also all 'leave basic review' menus will be terminated.");
        menuService.terminateMenuGroup(user, SEND_BASIC_REVIEW_TERMINATION.formatted(
                course.getId()));
        initiateAdvancedReview(review, confirmationMessage.getMessageId());
        return review;
    }

    @Override
    @NonNull
    public Review commitAdvancedReview(@NonNull Long reviewId,
            @NonNull LocalizedContent content) {
        final Review review = getReviewById(reviewId);
        if (review.getContent() != null) {
            throw new ActionExpiredException("Unable to submit content for basic review "
                    + reviewId + " because this review already has some content",
                    localizationLoader.getLocalizationForUser(
                    ERROR_COMMIT_ADVANCED_REVIEW_FAILURE, review.getUser()));
        }
        
        LOGGER.info("User " + review.getUser().getId() + " wants to submit an advanced review "
                + "for course " + review.getCourse().getName() + ". Content id is "
                + content.getId() + ".");  
        review.setAdvancedSubmittedTimestamp(LocalDateTime.now());
        review.setOriginalContent(content);
        review.setContent(content);

        LOGGER.info("Review object recompiled. Sending confirmation message...");
        final Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_ADVANCED_REVIEW_SUBMITTED, review.getUser(), PARAM_COURSE_NAME,
                review.getCourse().getName());
        client.sendMessage(review.getUser(), localization);
        LOGGER.info("Message sent. Updating review in the db..."); 
        reviewRepository.save(review);
        LOGGER.info("Review " + reviewId + " has been updated to include advanced feedback. "
                + "All advanced review menus will be terminated.");
        menuService.terminateMenuGroup(review.getUser(), SEND_ADVANCED_REVIEW_TERMINATION
                .formatted(review.getId()));
        return review;
    }

    @Override
    @NonNull
    public Review leaveComment(@NonNull UserEntity user, @NonNull Review review,
            @NonNull LocalizedContent content) {
        if (review.getCommentContent() != null) {
            throw new ActionExpiredException("Unable to submit comment content for review "
                    + review.getId() + " because this review already has a comment from user "
                    + review.getCommentedBy().getId(), localizationLoader.getLocalizationForUser(
                    ERROR_LEAVE_COMMENT_FAILURE, user));
        }
        
        LOGGER.info("User " + user.getId() + " wants to comment review " + review.getId() + ".");
        review.setCommentContent(content);
        review.setCommentedBy(user);
        review.setCommentedAt(LocalDateTime.now());

        LOGGER.info("Review object recompiled. Sending confirmation message...");
        final Localization success = localizationLoader.getLocalizationForUser(
                SERVICE_COMMENT_SUBMITTED, user, PARAM_REVIEW_ID, review.getId());
        client.sendMessage(user, success);
        LOGGER.info("Message sent. Updating review in the db..."); 
        reviewRepository.save(review);
        LOGGER.info("Review " + review.getId() + " has been updated to include comment.");

        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_COURSE_NAME, review.getCourse().getName());
        parameterMap.put(PARAM_COMMENTER_FULL_NAME, user.getFullName());

        LOGGER.info("Sending notification to the review's owner...");
        final Localization notification = localizationLoader.getLocalizationForUser(
                SERVICE_COMMENT_SUBMITTED_NOTIFICATION, user, parameterMap);
        client.sendMessage(review.getUser(), notification);
        LOGGER.info("Notificaton sent. Sending comment content...");
        contentService.sendContent(content, review.getUser());
        LOGGER.debug("Content sent. Marking review as read...");
        markReviewAsRead(review, user);
        LOGGER.info("Review marked as archived.");
        return review;
    }

    @Override
    @NonNull
    public Review updateCourseGrade(@NonNull Long reviewId, int newGrade) {
        final Review review = getReviewById(reviewId);

        LOGGER.info("User " + review.getUser().getId()
                + " wants to update their grade for course " + review.getCourse().getName()
                + ". Current grade is " + review.getCourseGrade() + " and new grade is "
                + newGrade + ".");
        review.setCourseGrade(newGrade);
        review.setLastUpdateTimestamp(LocalDateTime.now());

        LOGGER.info("Review object recompiled. Sending confirmation message...");
        final Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_REVIEW_COURSE_GRADE_UPDATED, review.getUser(), PARAM_COURSE_NAME,
                review.getCourse().getName());
        client.sendMessage(review.getUser(), localization);
        LOGGER.info("Message sent. Updating review in the db..."); 
        reviewRepository.save(review);
        LOGGER.info("Review " + reviewId + " has been updated.");
        return review;
    }

    @Override
    @NonNull
    public Review updatePlatformGrade(@NonNull Long reviewId, int newGrade) {
        final Review review = getReviewById(reviewId);

        LOGGER.info("User " + review.getUser().getId()
                + " wants to update their grade for platform in review " + review.getId()
                + ". Current grade is " + review.getPlatformGrade() + " and new grade is "
                + newGrade + ".");
        review.setPlatformGrade(newGrade);
        review.setLastUpdateTimestamp(LocalDateTime.now());

        LOGGER.info("Review object recompiled. Sending confirmation message...");
        final Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_REVIEW_PLATFORM_GRADE_UPDATED, review.getUser(), PARAM_COURSE_NAME,
                review.getCourse().getName());
        client.sendMessage(review.getUser(), localization);
        LOGGER.info("Message sent. Updating review in the db..."); 
        reviewRepository.save(review);
        LOGGER.info("Review " + reviewId + " has been updated.");
        return review;
    }

    @Override
    @NonNull
    public Review updateAdvancedReview(@NonNull Long reviewId,
            @NonNull LocalizedContent content) {
        final Review review = getReviewById(reviewId);
        if (review.getContent() == null) {
            throw new ForbiddenOperationException("Unable to update review " + reviewId
                    + "'s content because it has never been submitted", localizationLoader
                    .getLocalizationForUser(ERROR_UPDATE_CONTENT_NOT_PRESENT, review.getUser()));
        }

        LOGGER.info("User " + review.getUser().getId()
                + " wants to update their review's content for course " + review.getCourse()
                .getName() + ". Current content id is " + review.getContent().getId() + ".");
        review.setContent(content);
        review.setLastUpdateTimestamp(LocalDateTime.now());

        LOGGER.info("Review object recompiled. Sending confirmation message...");
        final Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_REVIEW_COURSE_CONTENT_UPDATED, review.getUser(), PARAM_COURSE_NAME,
                review.getCourse().getName());
        client.sendMessage(review.getUser(), localization);
        LOGGER.info("Message sent. Updating review in the db..."); 
        reviewRepository.save(review);
        LOGGER.info("Review " + reviewId + " has been updated.");
        return review;
    }

    @Override
    @NonNull
    public List<Review> getReviewsForCourse(@NonNull Course course, Pageable pageable) {
        return reviewRepository.findByCourseId(course.getId(), pageable);
    }

    @Override
    public void sendNewReviewsForUser(@NonNull UserEntity user) {
        final List<Review> reviews = reviewRepository.findNewReviewsForUser(
                user, PageRequest.of(0, pageSize));

        LOGGER.info("Sending " + reviews.size() + " new review(s) to user " + user.getId());
        sendReviews(reviews, user);
        LOGGER.info("Reviews sent. Updating review session...");

        updateReviewSession(user, reviews, false);
        LOGGER.info("Review session updated.");
    }

    @Override
    public void sendNewReviewsForUserAndCourse(@NonNull UserEntity user, @NonNull Long courseId) {
        final List<Review> reviews = reviewRepository.findNewReviewsForUserAndCourse(user,
                courseId, PageRequest.of(0, pageSize));

        LOGGER.info("Sending " + reviews.size() + " new review(s) for course " + courseId
                + " to user " + user.getId());
        sendReviews(reviews, user);
        LOGGER.info("Reviews sent. Updating review session...");

        updateReviewSession(user, reviews, true);
        LOGGER.info("Review session updated.");
    }

    @Override
    public void sendArchiveReviewsForUser(@NonNull UserEntity user) {
        final List<Review> archiveReviews = reviewRepository.findArchiveReviewsForUser(user);
        
        LOGGER.info("Sending " + archiveReviews.size() + " archive review(s) to user "
                + user.getId());
        sendArchiveReviews(archiveReviews, user, null);
        LOGGER.info("Reviews sent.");
    }

    @Override
    public void sendArchiveReviewsForUserAndCourse(@NonNull UserEntity user, @NonNull Long courseId) {
        final List<Review> archiveReviews = reviewRepository
                .findArchiveReviewsForUserAndCourse(user, courseId);
        
        LOGGER.info("Sending " + archiveReviews.size() + " archive review(s) for course "
                + courseId + " to user " + user.getId());
        sendArchiveReviews(archiveReviews, user, courseId);
        LOGGER.info("Reviews sent.");
    }

    @Override
    @NonNull
    public Review getReviewByCourseAndUser(@NonNull UserEntity user, @NonNull Course course) {
        return reviewRepository.findByCourseNameAndUserId(course.getName(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("User " + user.getId()
                + " has never left a review for course " + course.getName(), localizationLoader
                .getLocalizationForUser(ERROR_REVIEW_NOT_FOUND, user)));
    }

    @Override
    @NonNull
    public Review getReviewById(@NonNull Long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(() ->
                new EntityNotFoundException("Review with id " + reviewId + " does not exist.",
                localizationLoader.getLocalizationForUser(ERROR_REVIEW_NOT_FOUND, userService
                .getDiretor())));
    }

    @Override
    public boolean isBasicReviewForCourseAndUserAvailable(@NonNull UserEntity user,
            @NonNull Course course) {
        return reviewRepository.findByCourseNameAndUserId(course.getName(),
                user.getId()).isPresent();
    }

    @Override
    public boolean isAdvancedReviewForCourseAndUserAvailable(@NonNull UserEntity user,
            @NonNull Course course) {
        final Optional<Review> reviewOpt = reviewRepository.findByCourseNameAndUserId(
                course.getName(), user.getId());

        if (reviewOpt.isPresent()) {
            return reviewOpt.get().getContent() != null;
        }
        return false;
    }

    @Override
    @NonNull
    public Review markReviewAsRead(@NonNull Review review, @NonNull UserEntity user) {
        review.getMarkedAsReadBy().add(user);

        final ReviewSession currentReviewSession = CURRENT_REVIEW_COUNTER.get(user.getId());
        currentReviewSession.counter--;

        menuService.terminateMenuGroup(user, REVIEW_ACTIONS_MENU_TERMINATION
                .formatted(review.getId()));
        review = reviewRepository.save(review);

        if (currentReviewSession.counter < 1) {
            if (currentReviewSession.course != null) {
                sendNewReviewsForUserAndCourse(user, currentReviewSession.course.getId());
            } else {
                sendNewReviewsForUser(user);
            }
        }
        return review;
    }

    private void sendReviews(List<Review> reviews, UserEntity user) {
        if (reviews.isEmpty()) {
            LOGGER.info("No further reviews are availbable.");
            sendMessage(user, localizationLoader.getLocalizationForUser(
                    SERVICE_NO_NEW_REVIEWS_FOR_USER, user));
            return;
        }
        for (Review review : reviews) {
            LOGGER.info("Compiling review info for review " + review.getId() + ".");
            final Localization reviewInfo;
            
            final Message message;
            if (review.getContent() != null && review.getCommentContent() != null) {
                LOGGER.info("Review is advanced and has a comment.");
                reviewInfo = localizationLoader.getLocalizationForUser(
                    SERVICE_REVIEW_INFO_CONTENT_COMMENT, user, textUtil
                    .getParamsMapForNewReview(review));
                sendMessage(user, reviewInfo);
                final List<Message> sentMessages = contentService
                        .sendContent(review.getContent(), user);
                message = getMenuMessage(sentMessages, user);
            } else if (review.getContent() != null) {
                LOGGER.info("Review is advanced.");
                reviewInfo = localizationLoader.getLocalizationForUser(
                    SERVICE_REVIEW_INFO_CONTENT, user, textUtil.getParamsMapForNewReview(review));
                sendMessage(user, reviewInfo);
                final List<Message> sentMessages = contentService
                        .sendContent(review.getContent(), user);
                message = getMenuMessage(sentMessages, user);
            } else if (review.getCommentContent() != null) {
                LOGGER.info("Review has a comment.");
                reviewInfo = localizationLoader.getLocalizationForUser(
                    SERVICE_REVIEW_INFO_COMMENT, user, textUtil.getParamsMapForNewReview(review));
                message = sendMessage(user, reviewInfo);
            } else {
                LOGGER.info("Review is basic with no comment.");
                reviewInfo = localizationLoader.getLocalizationForUser(
                    SERVICE_REVIEW_INFO, user, textUtil.getParamsMapForNewReview(review));
                message = sendMessage(user, reviewInfo);
            }
            menuService.initiateMenu(REVIEW_ACTIONS_MENU, user, review.getId().toString(),
                    message.getMessageId());
            menuService.addToMenuTerminationGroup(user, user, message.getMessageId(),
                    REVIEW_ACTIONS_MENU_TERMINATION.formatted(review.getId()), null);
        }
    }

    private void sendArchiveReviews(List<Review> reviews, UserEntity user, Long courseId) {
        final Path tempFile = archiveReviewsDao.createTempFile(TEMP_FILE_NAME.formatted(
                user.getId()));
        final StringBuilder builder = new StringBuilder();

        for (Review review : reviews) {
            final String reviewInfo = textUtil.getArchiveReviewInfo(review, builder);
            LOGGER.info("Writing review " + review.getId() + " to a temp file "
                    + tempFile + "...");
            archiveReviewsDao.write(tempFile, reviewInfo);
            LOGGER.info("Review has been saved to the temp file.");
            builder.delete(0, builder.length());
        }
        final String fileName = ARCHIVE_REVIEWS_FILE_NAME.formatted(user.getId(),
                (user.getId() == null) ? "all" : courseId) + ARCHIVE_REVIEWS_FILE_FORMAT;
        try {
            LOGGER.info("Reading temp file " + tempFile + " and sending reviews file to user "
                    + user.getId() + "...");
            final InputStream inputStream = archiveReviewsDao.read(tempFile);
            try {
                client.execute(SendDocument.builder()
                        .chatId(user.getId())
                        .document(new InputFile(inputStream, fileName))
                        .build());
                LOGGER.info("File has been sent.");
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to send file " + fileName + " to user "
                        + user.getId(), localizationLoader.getLocalizationForUser(
                        ERROR_SEND_FILE_FAILURE, user), e);
            } finally {
                inputStream.close();
                LOGGER.info("Input stream has been closed.");
            }
        } catch (IOException e) {
            throw new ArchiveReviewsException("Unable to close the stream after the temp file "
                    + tempFile + " has been read for user " + user.getId(), null, e);
        }
    }

    private Message sendMessage(UserEntity user, Localization localization) {
        return client.sendMessage(user, localization);
    }

    private void updateReviewSession(UserEntity user, List<Review> reviews,
            boolean includeCourse) {
        final ReviewSession reviewSession = CURRENT_REVIEW_COUNTER.get(user.getId());
        if (reviewSession != null) {
            reviewSession.counter = reviews.size();
            reviewSession.course = (includeCourse && !reviews.isEmpty()) ? reviews.get(0)
                .getCourse() : null;
        } else {
            CURRENT_REVIEW_COUNTER.put(user.getId(), new ReviewSession((includeCourse &&
                    !reviews.isEmpty()) ? reviews.get(0).getCourse() : null, reviews.size()));
        }
    }

    private Message getMenuMessage(List<Message> sentMessages, UserEntity user) {
        final Message menuMessage;
        if (sentMessages.size() > 1) {
            LOGGER.debug("Review content is a media group. To avoid Telegram restrictions, an "
                    + "additional message will be sent to user " + user.getId()
                    + " to attach the feedback menu to.");
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .getLocalizationForUser(SERVICE_REVIEW_MEDIA_GROUP_BYPASS, user);
            menuMessage = client.sendMessage(user, mediaGroupBypassMessageLoc);
            LOGGER.debug("Additional message for menu has been sent.");
        } else {
            LOGGER.debug("Review content is not a media group. Menu will be attached to it.");    
            menuMessage = sentMessages.get(0);
        }
        return menuMessage;
    }

    private class ReviewSession {
        Course course;

        int counter;

        ReviewSession(Course course, int counter) {
            this.counter = counter;
            this.course = course;
        }
    }
}
