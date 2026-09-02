package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.dto.internal.CourseMenuDto;
import com.unbidden.telegramcoursesbot.dto.internal.MappingsByPositionInCourseCountDto;
import com.unbidden.telegramcoursesbot.dto.internal.UsersByCourseStageCountDto;
import com.unbidden.telegramcoursesbot.exception.CourseValidationException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.OnMaintenanceException;
import com.unbidden.telegramcoursesbot.exception.RefundImpossibleException;
import com.unbidden.telegramcoursesbot.exception.StaleStateException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseInvoice.PaymentType;
import com.unbidden.telegramcoursesbot.model.CourseOwnership;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.ExternalInvoice;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.TelegramInvoice;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipStatus;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.repository.CourseOwnershipRepository;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.repository.ReviewRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.PaymentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class CourseService {
    private static final Logger LOGGER = LogManager.getLogger(CourseService.class);

    private final CourseRepository courseRepository;

    private final CourseProgressRepository courseProgressRepository;

    private final ContentMappingRepository contentMappingRepository;

    private final CourseOwnershipRepository ownershipRepository;

    private final ReviewRepository reviewRepository;

    private final ContentOrchestrationService contentService;

    private final PaymentService paymentService;

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    private final ValidatorUtil validatorUtil;

    @Transactional(readOnly = true)
    public List<Course> getByBot(Bot bot) {
        Assert.notNull(bot, "bot cannot be null");

        final List<Course> courses = courseRepository.findByBotId(bot.getId());

        contentMappingRepository.findAllById(courses.stream().map(c -> c.getTitle().getId()).toList());

        return courses;
    }

    @Transactional(readOnly = true)
    public List<Course> getAllOwnedByUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        final List<Course> courses = courseRepository.findAllOwnedByUser(botRole.getUser().getId(), botRole.getBot().getId());

        contentMappingRepository.findAllById(courses.stream().map(c -> c.getTitle().getId()).toList());

        return courses;
    }

    @Transactional(readOnly = true)
    public List<Course> getAllAvailableByUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");
        
        final List<Course> courses = courseRepository.findAllAvailableToUser(botRole.getUser().getId(), botRole.getBot().getId());

        contentMappingRepository.findAllById(courses.stream().map(c -> c.getTitle().getId()).toList());
        
        return courses;
    }

    @Transactional(readOnly = true)
    public List<CourseMenuDto> getCourseMenuDtosForOwnedCourses(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        final List<CourseOwnership> ownerships = paymentService.getActiveOwnershipsForUserInBot(botRole);
        final List<Long> courseIds = ownerships.stream().map(co -> co.getCourse().getId()).toList();
        final List<CourseProgress> progresses = courseProgressRepository.findByUserIdAndCourseIdIn(botRole.getUser().getId(), courseIds);
        final List<Review> reviews = reviewRepository.findByUserIdAndCourseIdIn(botRole.getUser().getId(), courseIds);
        final List<CourseMenuDto> dtos = new ArrayList<>();

        for (final var ownership : ownerships) {
            final Optional<CourseProgress> progressOpt = progresses.stream()
                    .filter(p -> p.getCourse().getId().equals(ownership.getCourse().getId()))
                    .findAny();
            final Optional<Review> reviewOpt = reviews.stream()
                    .filter(r -> r.getCourse().getId().equals(ownership.getCourse().getId()))
                    .findAny();

            dtos.add(getCourseMenuDto(botRole, ownership, progressOpt, reviewOpt));
        }

        return dtos;
    }

    @Transactional(readOnly = true)
    public CourseMenuDto getCourseMenuDtoForCourse(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseOwnership ownership = entityUtil.getActiveCourseOwnership(botRole, courseId);
        final Optional<CourseProgress> progressOpt = courseProgressRepository.findByUserIdAndCourseId(botRole.getUser().getId(), courseId);
        final Optional<Review> reviewOpt = reviewRepository.findByUserIdAndCourseId(botRole.getUser().getId(), courseId);

        return getCourseMenuDto(botRole, ownership, progressOpt, reviewOpt);
    }

    @Transactional(readOnly = true)
    public boolean hasCourseBeenCompleted(UserEntity user, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Optional<CourseProgress> progressOpt = courseProgressRepository
                .findByUserIdAndCourseId(user.getId(), courseId);

        if (progressOpt.isPresent()) {
            return progressOpt.get().getNumberOfTimesCompleted() > 0;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<UsersByCourseStageCountDto> countAndGroupByCourseStage(Long courseId) {
        Assert.notNull(courseId, "courseId cannot be null");

        return courseProgressRepository.countAndGroupByCourseStage(courseId);
    }

    @Transactional
    public void checkDeletable(BotRole botRole, Long courseId) {
        final long activeOwnerships = ownershipRepository.countByCourseIdAndStatus(courseId, OwnershipStatus.ACTIVE);

        if (activeOwnerships > 0) {
            throw new ForbiddenOperationException("Courses that have active ownerships cannot be deleted.",
                    localizationLoader.localize(Localizations.Error.DELETE_COURSE_ACTIVE_OWNERSHIPS, botRole,
                        new Localizations.Error.DeleteCourseActiveOwnershipsParams(activeOwnerships)));
        }
    }

    @Transactional
    public CourseProgress createOrLoadProgress(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Optional<CourseProgress> progressOpt = courseProgressRepository
                .findByUserIdAndCourseId(botRole.getUser().getId(), courseId);

        final CourseProgress progress;
        if (progressOpt.isPresent()) {
            progress = progressOpt.get();
            LOGGER.info("User " + botRole.getUser().getId() + " already has a course progress for course "
                    + courseId + ".");    

            return progress;
        } else {
            progress = new CourseProgress();
            progress.setUser(botRole.getUser());
            progress.setCourse(entityUtil.getCourseById(botRole, courseId));
            progress.setStage(0);
            progress.setFirstTimeStartedAt(LocalDateTime.now());
            progress.setNumberOfTimesCompleted(0);
            LOGGER.info("New course progress for user " + botRole.getUser().getId() + " and course "
                    + courseId + " has been set up.");
            return courseProgressRepository.save(progress);
        }
    }

    @Transactional
    public Course createCourse(BotRole botRole, Long titleContentId, PaymentType paymentType,
            String languageCode, List<Message> invoiceMessages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(titleContentId, "titleContentId cannot be null");
        Assert.notNull(paymentType, "paymentType cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notEmpty(invoiceMessages, "messages cannot be empty or null");

        final Course course = new Course();
        final ContentMapping titleMapping = new ContentMapping();

        titleMapping.setPosition(0);
        titleMapping.setContent(List.of(entityUtil.getLocalizedContentReference(titleContentId)));

        course.setTitle(contentMappingRepository.save(titleMapping));
        course.setBot(botRole.getBot());
        course.setLessons(List.of());
        course.setUnderMaintenance(true);
        course.setFeedbackIncluded(true);
        course.setHomeworkIncluded(true);
        final ContentMapping invoiceMapping = new ContentMapping();

        invoiceMapping.setPosition(0);
        if (paymentType == PaymentType.TELEGRAM) {
            validatorUtil.checkExactExpectedMessages(botRole, invoiceMessages, 2);
            final Integer price = validatorUtil.parseIntInBounds(botRole, invoiceMessages.getLast(), 1, PaymentOrchestrationService.MAX_PRICE);

            invoiceMessages.removeLast();
            invoiceMapping.setContent(List.of(contentService.parseAndPersistContent(botRole, invoiceMessages, languageCode, List.of(MediaType.TEXT))));
            course.setInvoice(new TelegramInvoice(contentMappingRepository.save(invoiceMapping), price, null));
        } else {
            validatorUtil.checkAtLeastExpectedMessages(botRole, invoiceMessages, 2);
            final URI uri = validatorUtil.checkUri(botRole, invoiceMessages.getLast());

            invoiceMessages.removeLast();
            invoiceMapping.setContent(List.of(contentService.parseAndPersistContent(botRole, invoiceMessages, languageCode)));
            course.setInvoice(new ExternalInvoice(uri.toString(), contentMappingRepository.save(invoiceMapping)));
        }

        courseRepository.save(course);
        LOGGER.debug("New course " + course.getId() + " has been created.");

        return course;
    }
    
    @Transactional(readOnly = true)
    public void checkCourseIsNotUnderMaintenance(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        if (courseRepository.existsByIdAndIsUnderMaintenanceTrue(courseId)) {
            throw new OnMaintenanceException("Course " + courseId + " is currently "
                    + "marked as under maintenance", localizationLoader.localize(
                    Error.COURSE_UNDER_MAINTENANCE, botRole));
        }
    }

    @Transactional
    public CourseProgress incrementStage(BotRole botRole, Long courseId, Long currentLessonId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(currentLessonId, "currentLessonId cannot be null");

        final CourseProgress progress = entityUtil.getCourseProgressForUser(botRole, courseId);
        final Lesson currentLesson = entityUtil.getLessonById(botRole, currentLessonId);

        if (!progress.getStage().equals(currentLesson.getPosition())) {
            throw new StaleStateException("Cannot advance to the next lesson because the sent request is stale.",
                    localizationLoader.localize(Localizations.Error.FAILED_ADVANCE_TO_NEXT_LESSON, botRole));
        }

        LOGGER.debug("Current stage in course " + courseId + " for user " + botRole.getUser().getId() + " is "
                + progress.getStage() + ". Incrementing by 1...");
        progress.setStage(progress.getStage() + 1);

        return progress;
    }

    @Transactional
    public CourseProgress setCourseProgressStage(BotRole botRole, Long courseId, int newStage) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseProgress progress = entityUtil.getCourseProgressForUser(botRole, courseId);

        LOGGER.debug("Current stage for user " + botRole.getUser().getId() + " in course " + courseId + " is "
                + progress.getStage() + ". Setting it to " + newStage + "...");
        progress.setStage(newStage);

        return progress;
    }

    @Transactional
    public CourseProgress resetCourseProgress(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseProgress progress = entityUtil.getCourseProgressForUser(botRole, courseId);

        if (progress.getNumberOfTimesCompleted() < 1) {
            progress.setFirstTimeFinishedAt(LocalDateTime.now());
        }
        progress.setNumberOfTimesCompleted(progress.getNumberOfTimesCompleted() + 1);
        progress.setStage(0);

        return progress;
    }

    @Transactional
    public Course toggleMaintenance(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(botRole, courseId);

        LOGGER.info("User " + botRole.getUser().getId() + " is trying to toggle maintenance for course "
                + courseId + "... Current status is " + getStatus(course.isUnderMaintenance()) + ".");

        if (course.isUnderMaintenance()) {
            LOGGER.debug("User " + botRole.getUser().getId() + " wants to disable maintenance for course " + courseId + ". Validating...");

            if (course.getLessons().size() < 1) {
                throw new CourseValidationException("Course must have at least one lesson.", localizationLoader
                        .localize(Localizations.Error.COURSE_VALIDATION_NO_LESSONS, botRole));
            }
            final List<MappingsByPositionInCourseCountDto> countDtos = contentMappingRepository.countAndGroupByPositionInLessonsInCourse(courseId);

            LOGGER.info("Count dtos: " + countDtos + ".");
            for (final var dto : countDtos) {
                if (dto.numberOfMappings() < 1) {
                    throw new CourseValidationException("Lessons must have at least one content mapping.", localizationLoader
                            .localize(Localizations.Error.COURSE_VALIDATION_NO_CONTENT_IN_LESSON, botRole,
                                new Localizations.Error.CourseValidationNoContentInLessonParams(dto.position())));
                }
            }
        }
        
        course.setUnderMaintenance(!course.isUnderMaintenance());
        
        LOGGER.info("Course " + course.getId() + "'s maintenance status is now "
                + getStatus(course.isUnderMaintenance()) + ".");

        return course;
    }

    @Transactional
    public Course toggleFeedbackInclusion(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(botRole, courseId);

        LOGGER.info("User " + botRole.getUser().getId() + " is trying to toggle feedback inclusion for course "
                + courseId + "... Current status is " + getStatus(course.isFeedbackIncluded()) + ".");
        
        course.setFeedbackIncluded(!course.isFeedbackIncluded());
        
        LOGGER.info("Feedback inclusion for course " + courseId + " is now " + getStatus(course.isFeedbackIncluded()) + ".");

        return course;
    }

    @Transactional
    public Course toggleHomeworkInclusion(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(botRole, courseId);

        course.setHomeworkIncluded(!course.isHomeworkIncluded());
        
        LOGGER.info("Homework inclusion for course " + courseId + " is now " + getStatus(course.isHomeworkIncluded()) + ".");

        return course;
    }

    @Transactional
    public Course updateCoursePrice(BotRole botRole, Long courseId, int newPrice) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        if (newPrice <= 0 || newPrice > PaymentOrchestrationService.MAX_PRICE) {
            throw new InvalidDataSentException("Course price must be greater than 0 and lower than or equal to "
                    + PaymentOrchestrationService.MAX_PRICE + ".", localizationLoader.localize(
                        Localizations.Error.PARSE_INT_BOUNDS_FAILURE, botRole,
                        new Localizations.Error.ParseIntBoundsFailureParams(1, PaymentOrchestrationService.MAX_PRICE)));
        }

        final Course course = entityUtil.getCourseById(botRole, courseId);

        if (!course.getInvoice().getClass().equals(TelegramInvoice.class)) {
            throw new ForbiddenOperationException("Course " + courseId + " uses external payments. Payment type "
                    + "must be changed first before updating its price.", localizationLoader.localize(
                        Localizations.Error.COURSE_PRICE_UPDATE_EXTERNAL_INVOICE, botRole));
        }
        final TelegramInvoice invoice = (TelegramInvoice)course.getInvoice();

        LOGGER.info("User " + botRole.getUser().getId() + " is changing price for course " + courseId + ". Current value is: "
                + invoice.getPrice() + ". Updating to " + newPrice + ".");

        invoice.setPrice(newPrice);

        return course;
    }

    @Transactional
    public Course updateRefundStage(BotRole botRole, Long courseId, int newStage) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(botRole, courseId);

        if (newStage >= course.getLessons().size()) {
            throw new InvalidDataSentException("Provided new refund stage " + newStage + " is greater than the number "
                    + "of lessons in course " + courseId + ".", localizationLoader.localize(
                        Localizations.Error.REFUND_STAGE_GREATER_THAN_NUMBER_OF_LESSONS, botRole,
                        new Localizations.Error.RefundStageGreaterThanNumberOfLessonsParams(course.getLessons().size() - 1)));
        }
        if (!course.getInvoice().getClass().equals(TelegramInvoice.class)) {
            throw new ForbiddenOperationException("Course " + courseId + " uses external payments. Payment type "
                    + "must be changed first before updating its refund stage.", localizationLoader.localize(
                        Localizations.Error.COURSE_REFUND_STAGE_UPDATE_EXTERNAL_INVOICE, botRole));
        }
        final TelegramInvoice invoice = (TelegramInvoice)course.getInvoice();

        if (newStage < 0 && invoice.getRefundStage() == null || newStage == invoice.getRefundStage()) {
            throw new InvalidDataSentException("New refund stage is the same as before.",
                    localizationLoader.localize(Localizations.Error.SAME_NEW_REFUND_STAGE, botRole));
        }

        LOGGER.info("User " + botRole.getUser().getId() + " is changing refund stage for course " + courseId
                + ". Current value is: " + invoice.getRefundStage() + ". Updating to " + newStage + ".");

        invoice.setRefundStage(newStage < 0 ? null : newStage);

        return course;
    }

    @Transactional
    public void deleteCourse(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        checkDeletable(botRole, courseId);

        courseRepository.deleteById(courseId);
    }
    
    private String getStatus(boolean status) {
        return status ? "ENABLED" : "DISABLED";
    }

    private CourseMenuDto getCourseMenuDto(BotRole botRole, CourseOwnership ownership,
            Optional<CourseProgress> progressOpt, Optional<Review> reviewOpt) {
        final boolean isCompleted = progressOpt.isPresent() ? progressOpt.get().getNumberOfTimesCompleted() > 0 : false;

        boolean isRefundable = false;
        if (!isCompleted) {
            if (progressOpt.isPresent()) {
                try {
                    paymentService.checkRefundPossible(botRole, ownership.getCourse(), ownership, progressOpt.get());
                    isRefundable = true;
                } catch (RefundImpossibleException e) {
                    isRefundable = false;
                }
            } else {
                isRefundable = ownership.getCourse().getInvoice().getClass().equals(TelegramInvoice.class)
                        && ((TelegramInvoice)ownership.getCourse().getInvoice()).getRefundStage() != null;
            }
        }

        final boolean isBasicReviewPresent;
        final boolean isAdvancedReviewPresent;
        if (reviewOpt.isPresent()) {
            final Review review = reviewOpt.get();

            isBasicReviewPresent = true;
            isAdvancedReviewPresent = review.getContent() != null;
        } else {
            isBasicReviewPresent = false;
            isAdvancedReviewPresent = false;
        }
        
        return new CourseMenuDto(
                ownership.getCourse().getId(),
                contentService.getLocalizedText(botRole, ownership.getCourse().getTitle()),
                isCompleted,
                isRefundable,
                isBasicReviewPresent,
                isAdvancedReviewPresent);
    }
}
