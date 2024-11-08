package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.repository.LessonRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private static final Logger LOGGER = LogManager.getLogger(CourseServiceImpl.class);

    private static final String NEXT_STAGE_MENU = "m_crsNxtStg";
    
    private static final String TEST_COURSE_NAME = "test_course";

    private static final String SERVICE_COURSE_NEXT_STAGE_MEDIA_GROUP_BYPASS =
            "service_course_next_stage_media_group_bypass";

    private static final String COURSE_END = "course_%s_end";
    private static final String COURSE_END_REPEAT = "course_%s_end_repeat";

    private static final String ERROR_COURSE_PROGRESS_NOT_FOUND =
            "error_course_progress_not_found";
    private static final String ERROR_COURSE_NOT_FOUND = "error_course_not_found";

    private final CourseRepository courseRepository;

    private final LessonRepository lessonRepository;

    private final CourseProgressRepository courseProgressRepository;

    private final PaymentService paymentService;

    private final MenuService menuService;

    private final UserService userService;

    private final HomeworkService homeworkService;

    private final ReviewService reviewService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Value("${telegram.bot.course.show-test-course}")
    private Boolean isTestCourseShown;

    @Override
    public void initMessage(@NonNull User user, @NonNull String courseName) {
        initMessage(userService.getUser(user.getId()), courseName);
    }

    @Override
    public void initMessage(@NonNull UserEntity user, @NonNull String courseName) {
        if (!paymentService.isAvailable(user, courseName)) {
            paymentService.sendInvoice(user, courseName);
            return;
        }
        LOGGER.info("Course " + courseName + " is available for user " + user.getId() + ".");
        final Course course = getCourseByName(courseName, user);
        final Optional<CourseProgress> progressOpt = courseProgressRepository
                .findByUserIdAndCourseName(user.getId(), courseName);

        CourseProgress progress;
        if (progressOpt.isPresent()) {
            progress = progressOpt.get();
            LOGGER.info("User " + user.getId() + " already has a course progress for course "
                    + courseName + ".");
        } else {
            progress = new CourseProgress();
            progress.setUser(userService.getUser(user.getId()));
            progress.setCourse(course);
            progress.setStage(0);
            progress.setFirstTimeStartedAt(LocalDateTime.now());
            progress.setNumberOfTimesCompleted(0);
            LOGGER.info("New course progress for user " + user.getId() + " and course "
                    + courseName + " has been set up. Saving...");
            courseProgressRepository.save(progress);
            LOGGER.info("Course progress saved.");
        }
        current(course, progress);
    }

    @Override
    public void next(@NonNull UserEntity user, @NonNull String courseName) {
        final CourseProgress progress = courseProgressRepository.findByUserIdAndCourseName(
                user.getId(), courseName).orElseThrow(() -> new EntityNotFoundException(
                "Course progress for course " + courseName + " and user " + user.getId()
                + " does not exist", localizationLoader.getLocalizationForUser(
                ERROR_COURSE_PROGRESS_NOT_FOUND, user)));

        LOGGER.info("Current stage in course " + courseName + " for user " + user.getId() + " is "
                + progress.getStage() + ". Incrementing by 1...");
        progress.setStage(progress.getStage() + 1);
        courseProgressRepository.save(progress);
        LOGGER.info("Course stage incremented and progress saved.");

        if (progress.getStage().equals(progress.getCourse().getAmountOfLessons())) {
            LOGGER.info("User " + user.getId() + " has completed course " + courseName
                    + ". Commencing ending sequence...");
            end(user, progress);
            return;
        }
        current(progress.getCourse(), progress);
    }

    @Override
    public void current(@NonNull Course course, @NonNull CourseProgress courseProgress) {
        final Lesson lesson = lessonRepository.findByPositionAndCourseName(
                courseProgress.getStage(), course.getName()).get();
        final UserEntity user = courseProgress.getUser();

        LOGGER.debug("Sending content for lesson " + lesson.getId() + " to user "
                + user.getId() + "...");
        for (int i = 0; i < lesson.getStructure().size() - 1; i++) {
            contentService.sendLocalizedContent(lesson.getStructure().get(i), user);
        }
        LOGGER.debug("All except last content has been sent.");
        final List<Message> lastContent = contentService.sendLocalizedContent(
                lesson.getStructure().get(lesson.getStructure().size() - 1), user);
        LOGGER.debug("Last content has been sent.");
        if (course.isHomeworkIncluded() && lesson.isHomeworkIncluded()) {
            LOGGER.debug("Lesson " + lesson.getId() + " includes homework. "
                    + "Commencing homework sequence...");
            homeworkService.sendHomework(user, homeworkService.getHomework(
                    lesson.getHomework().getId(), courseProgress.getUser()));
            return;
        }
        
        LOGGER.debug("Lesson " + lesson.getId() + " or the course does not have any homework."
                + " Checking, if this is the last lesson...");
        if (courseProgress.getStage().equals(course.getAmountOfLessons() - 1)) {
            LOGGER.info("User " + user.getId() + " has completed course " + course.getName()
                    + ". Commencing ending sequence...");
            end(user, courseProgress);
            return;
        }
        LOGGER.debug("This is not the last lesson. Initiating next stage menu...");
        final Message menuMessage;
        if (lastContent.size() > 1) {
            LOGGER.warn("Last content in lesson " + lesson.getId() + " is a media group. "
                    + "It is a recomendation to avoid such cases since it requires an "
                    + "additional message to be sent for menu.");
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .getLocalizationForUser(SERVICE_COURSE_NEXT_STAGE_MEDIA_GROUP_BYPASS, user);
            menuMessage = client.sendMessage(user, mediaGroupBypassMessageLoc);
            LOGGER.debug("Additional message for menu has been sent.");
        } else {
            LOGGER.debug("Last message in lesson " + lesson.getId() + " is not a media group. "
                    + "Menu will be attached to it.");    
            menuMessage = lastContent.get(0);
        }
        menuService.initiateMenu(NEXT_STAGE_MENU, user, course.getName()
                + COURSE_NAME_LESSON_INDEX_DIVIDER + courseProgress.getStage(),
                menuMessage.getMessageId());
        menuService.addToMenuTerminationGroup(user, user, menuMessage.getMessageId(),
                COURSE_NEXT_STAGE_MENU_TERMINATION.formatted(courseProgress.getId()), null);
        LOGGER.debug("Next lesson menu sent.");
    }

    @Override
    public void end(@NonNull UserEntity user, @NonNull CourseProgress courseProgress) {
        courseProgress.setNumberOfTimesCompleted(courseProgress.getNumberOfTimesCompleted() + 1);
        if (courseProgress.getNumberOfTimesCompleted() > 1) {
            LOGGER.info("User " + user.getId() + " has completed course "
                    + courseProgress.getCourse().getName() + " for the "
                    + courseProgress.getNumberOfTimesCompleted() + " time!");
        } else {
            LOGGER.info("User " + user.getId() + " has completed course "
                    + courseProgress.getCourse().getName() + " for the first time!");
        }
        courseProgress.setStage(0);
        final Localization localization = (courseProgress.getNumberOfTimesCompleted() > 1)
                ? localizationLoader.getLocalizationForUser(COURSE_END_REPEAT.formatted(
                    courseProgress.getCourse().getName()), user) : localizationLoader
                    .getLocalizationForUser(COURSE_END.formatted(courseProgress.getCourse()
                    .getName()), user);
        courseProgressRepository.save(courseProgress);
        client.sendMessage(user, localization);
        if (!reviewService.isBasicReviewForCourseAndUserAvailable(user,
                courseProgress.getCourse())) {
            reviewService.initiateBasicReview(user, courseProgress.getCourse());
        }
    }

    @Override
    @NonNull
    public Course getCourseByName(@NonNull String courseName, @NonNull UserEntity user) {
        return courseRepository.findByName(courseName).orElseThrow(() ->
                new EntityNotFoundException("Course " + courseName + " does not exist",
                localizationLoader.getLocalizationForUser(ERROR_COURSE_NOT_FOUND, user)));
    }

    @Override
    @NonNull
    public Course getCourseById(@NonNull Long id, @NonNull UserEntity user) {
        return courseRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Course " + id + " does not exist",
                localizationLoader.getLocalizationForUser(ERROR_COURSE_NOT_FOUND, user)));
    }

    @Override
    @NonNull
    public List<Course> getAll() {
        return (isTestCourseShown) ? courseRepository.findAll() : courseRepository.findAll()
                .stream().filter(c -> !c.getName().equals(TEST_COURSE_NAME)).toList();
    }

    @Override
    @NonNull
    public List<Course> getAllOwnedByUser(@NonNull UserEntity user) {
        final List<PaymentDetails> paymentDetails = paymentService.getAllForUser(user);

        return (isTestCourseShown) ? paymentDetails.stream()         
                .map(pd -> pd.getCourse()).toList() : paymentDetails.stream()
                .filter(pd -> !pd.getCourse().getName().equals(TEST_COURSE_NAME))
                .map(pd -> pd.getCourse()).toList();
    }

    @Override
    @NonNull
    public Course save(@NonNull Course course) {
        return courseRepository.save(course);
    }

    @Override
    public boolean hasCourseBeenCompleted(@NonNull UserEntity user, @NonNull Course course) {
        final Optional<CourseProgress> progressOpt = courseProgressRepository
                .findByUserIdAndCourseName(user.getId(), course.getName());

        if (progressOpt.isPresent()) {
            return progressOpt.get().getNumberOfTimesCompleted() > 0;
        }
        return false;
    }

    @Override
    @NonNull
    public CourseProgress getCurrentCourseProgressForUser(@NonNull Long userId, @NonNull String courseName) {
        final UserEntity user = userService.getUser(userId);
        return courseProgressRepository.findByUserIdAndCourseName(userId, courseName)
                .orElseThrow(() -> new EntityNotFoundException("Course progress for user "
                + userId + " and course " + courseName, localizationLoader.getLocalizationForUser(
                ERROR_COURSE_PROGRESS_NOT_FOUND, user)));
    }
    
    @Override
    public void delete(@NonNull Course course) {
        courseRepository.delete(course);
    }
}
