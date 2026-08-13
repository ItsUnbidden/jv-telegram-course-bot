package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.dto.internal.CourseMenuDto;
import com.unbidden.telegramcoursesbot.dto.internal.UsersByCourseStageCountDto;
import com.unbidden.telegramcoursesbot.exception.OnMaintenanceException;
import com.unbidden.telegramcoursesbot.exception.RefundImpossibleException;
import com.unbidden.telegramcoursesbot.exception.StaleStateException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseOwnership;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.repository.ReviewRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
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

    // TODO: private static final String ERROR_CANNOT_DELETE_BOUGHT_COURSE = "error_cannot_delete_bought_course";
    private final CourseRepository courseRepository;

    private final CourseProgressRepository courseProgressRepository;

    private final ContentMappingRepository contentMappingRepository;

    private final ReviewRepository reviewRepository;

    private final ContentService contentService;

    private final PaymentService paymentService;

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    @Transactional(readOnly = true)
    public List<Course> getByBot(Bot bot) {
        Assert.notNull(bot, "bot cannot be null");

        return courseRepository.findByBotId(bot.getId());
    }

    @Transactional(readOnly = true)
    public List<Course> getAllOwnedByUser(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        return courseRepository.findAllOwnedByUser(user.getId(), bot.getId());
    }

    @Transactional(readOnly = true)
    public List<Course> getAllAvailableByUser(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        
        return courseRepository.findAllAvailableToUser(user.getId(), bot.getId());
    }

    @Transactional(readOnly = true)
    public List<CourseMenuDto> getCourseMenuDtosForOwnedCourses(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final List<CourseOwnership> ownerships = paymentService.getActiveOwnershipsForUserInBot(user, bot);
        final List<Long> courseIds = ownerships.stream().map(co -> co.getCourse().getId()).toList();
        final List<CourseProgress> progresses = courseProgressRepository.findByUserIdAndCourseIdIn(user.getId(), courseIds);
        final List<Review> reviews = reviewRepository.findByUserIdAndCourseIdIn(user.getId(), courseIds);
        final List<CourseMenuDto> dtos = new ArrayList<>();

        for (final var ownership : ownerships) {
            final Optional<CourseProgress> progressOpt = progresses.stream()
                    .filter(p -> p.getCourse().getId().equals(ownership.getCourse().getId()))
                    .findAny();
            final Optional<Review> reviewOpt = reviews.stream()
                    .filter(r -> r.getCourse().getId().equals(ownership.getCourse().getId()))
                    .findAny();

            dtos.add(getCourseMenuDto(user, bot, ownership, progressOpt, reviewOpt));
        }

        return dtos;
    }

    @Transactional(readOnly = true)
    public CourseMenuDto getCourseMenuDtoForCourse(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseOwnership ownership = entityUtil.getActiveCourseOwnership(user, bot, courseId);
        final Optional<CourseProgress> progressOpt = courseProgressRepository.findByUserIdAndCourseId(user.getId(), courseId);
        final Optional<Review> reviewOpt = reviewRepository.findByCourseIdAndUserId(courseId, user.getId());

        return getCourseMenuDto(user, bot, ownership, progressOpt, reviewOpt);
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
        return courseProgressRepository.countAndGroupByCourseStage(courseId);
    }

    @Transactional
    public CourseProgress createOrLoadProgress(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final Optional<CourseProgress> progressOpt = courseProgressRepository
                .findByUserIdAndCourseId(user.getId(), courseId);

        final CourseProgress progress;
        if (progressOpt.isPresent()) {
            progress = progressOpt.get();
            LOGGER.info("User " + user.getId() + " already has a course progress for course "
                    + courseId + ".");    

            return progress;
        } else {
            progress = new CourseProgress();
            progress.setUser(user);
            progress.setCourse(entityUtil.getCourseById(user, bot, courseId));
            progress.setStage(0);
            progress.setFirstTimeStartedAt(LocalDateTime.now());
            progress.setNumberOfTimesCompleted(0);
            LOGGER.info("New course progress for user " + user.getId() + " and course "
                    + courseId + " has been set up.");
            return courseProgressRepository.save(progress);
        }
    }

    @Transactional
    public Course createCourse(UserEntity user, Bot bot, List<Message> titleMessages) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(titleMessages, "messages cannot be null");

        final Course course = new Course();
        final ContentMapping titleMapping = new ContentMapping();

        titleMapping.setPosition(0);
        titleMapping.setContent(List.of(contentService.parseAndPersistContent(user, bot, titleMessages)));

        course.setTitle(contentMappingRepository.save(titleMapping));
        course.setNumberOfLessons(0);
        course.setRefundStage(-1);
        course.setBot(bot);
        course.setUnderMaintenance(true);

        course.setLessons(List.of());
        course.setFeedbackIncluded(true);
        course.setHomeworkIncluded(true);
        
        courseRepository.save(course);
        LOGGER.debug("New course " + course.getId() + " has been created.");
        return course;
    }
    
    @Transactional(readOnly = true)
    public void checkCourseIsNotUnderMaintenance(UserEntity user, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        if (courseRepository.existsByIdAndIsUnderMaintenanceTrue(courseId)) {
            throw new OnMaintenanceException("Course " + courseId + " is currently "
                    + "marked as under maintenance", localizationLoader.localize(
                    Error.COURSE_UNDER_MAINTENANCE, user));
        }
    }

    @Transactional
    public CourseProgress incrementStage(UserEntity user, Bot bot, Long courseId, Long currentLessonId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseProgress progress = entityUtil.getCourseProgressForUser(user, bot, courseId);
        final Lesson currentLesson = entityUtil.getLessonById(user, bot, currentLessonId);

        if (!progress.getStage().equals(currentLesson.getPosition())) {
            throw new StaleStateException("Cannot advance to the next lesson because the sent request is stale.",
                    localizationLoader.localize(Localizations.Error.FAILED_ADVANCE_TO_NEXT_LESSON, user));
        }

        LOGGER.debug("Current stage in course " + courseId + " for user " + user.getId() + " is "
                + progress.getStage() + ". Incrementing by 1...");
        progress.setStage(progress.getStage() + 1);

        return progress;
    }

    @Transactional
    public CourseProgress setCourseProgressStage(UserEntity user, Bot bot, Long courseId, int newStage) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseProgress progress = entityUtil.getCourseProgressForUser(user, bot, courseId);

        LOGGER.debug("Current stage for user " + user.getId() + " in course " + courseId + " is "
                + progress.getStage() + ". Setting it to " + newStage + "...");
        progress.setStage(newStage);

        return progress;
    }

    @Transactional
    public CourseProgress resetCourseProgress(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseProgress progress = entityUtil.getCourseProgressForUser(user, bot, courseId);

        progress.setNumberOfTimesCompleted(progress.getNumberOfTimesCompleted() + 1);
        progress.setStage(0);

        return progress;
    }

    private CourseMenuDto getCourseMenuDto(UserEntity user, Bot bot, CourseOwnership ownership,
            Optional<CourseProgress> progressOpt, Optional<Review> reviewOpt) {
        final boolean isCompleted = progressOpt.isPresent() ? progressOpt.get().getNumberOfTimesCompleted() > 0 : false;

        boolean isRefundable = false;
        if (!isCompleted) {
            if (progressOpt.isPresent()) {
                try {
                    paymentService.checkRefundPossible(user, bot, ownership.getCourse(), ownership, progressOpt.get());
                    isRefundable = true;
                } catch (RefundImpossibleException e) {
                    isRefundable = false;
                }
            } else {
                isRefundable = ownership.getCourse().getRefundStage() >= 0;
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
                contentService.getLocalizedText(user, bot, ownership.getCourse().getTitle()),
                isCompleted,
                isRefundable,
                isBasicReviewPresent,
                isAdvancedReviewPresent);
    }
}
