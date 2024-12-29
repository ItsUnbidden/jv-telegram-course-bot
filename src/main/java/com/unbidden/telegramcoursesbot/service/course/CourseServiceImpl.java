package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.AccessDeniedException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.OnMaintenanceException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.LessonTrigger;
import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import com.unbidden.telegramcoursesbot.model.TimedTrigger;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.repository.LessonRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private static final Logger LOGGER = LogManager.getLogger(CourseServiceImpl.class);
    
    private static final String NEXT_STAGE_MENU = "m_crsNxtStg";
    
    private static final int TEST_COURSE_AMOUNT_OF_LESSONS = 2;
    private static final int TEST_COURSE_PRICE = 1;
    private static final String TEST_COURSE_NAME = "test_course";

    private static final String PARAM_TIME_LEFT = "${timeLeft}";

    private static final String SERVICE_COURSE_NEXT_STAGE_MEDIA_GROUP_BYPASS =
            "service_course_next_stage_media_group_bypass";

    private static final String COURSE_END = "course_%s_end";
    private static final String COURSE_END_REPEAT = "course_%s_end_repeat";

    private static final String ERROR_COURSE_PROGRESS_NOT_FOUND =
            "error_course_progress_not_found";
    private static final String ERROR_COURSE_NOT_FOUND = "error_course_not_found";
    private static final String ERROR_AWAITING_LESSON = "error_awaiting_lesson";
    private static final String ERROR_BOT_VISIBILITY_MISMATCH = "error_bot_visibility_mismatch";
    private static final String ERROR_CANNOT_DELETE_BOUGHT_COURSE =
            "error_cannot_delete_bought_course";
    private static final String ERROR_COURSE_UNDER_MAINTENANCE = "error_course_under_maintenance";
    private static final String ERROR_NO_CONTENT_IN_LESSON = "error_no_content_in_lesson";

    private final CourseRepository courseRepository;

    private final LessonRepository lessonRepository;

    private final CourseProgressRepository courseProgressRepository;

    private final PaymentService paymentService;

    private final MenuService menuService;

    private final UserService userService;

    private final HomeworkService homeworkService;

    private final ReviewService reviewService;

    private final ContentService contentService;

    private final TimingService timingService;

    private final BotService botService;

    private final LocalizationLoader localizationLoader;

    private final TextUtil textUtil;

    private final ClientManager clientManager;

    @Override
    public void initMessage(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull String courseName) {
        final Course course = getCourseByName(courseName, user, bot);
        checkCourseIsNotUnderMaintenance(course, user);

        if (!paymentService.isAvailable(user, bot, courseName)) {
            paymentService.sendInvoice(user, bot, courseName);
            return;
        }
        LOGGER.info("Course " + courseName + " is available for user " + user.getId() + ".");
        final Optional<CourseProgress> progressOpt = courseProgressRepository
                .findByUserIdAndCourseName(user.getId(), courseName);

        CourseProgress progress;
        if (progressOpt.isPresent()) {
            progress = progressOpt.get();
            LOGGER.info("User " + user.getId() + " already has a course progress for course "
                    + courseName + ".");
        } else {
            progress = new CourseProgress();
            progress.setUser(user);
            progress.setCourse(course);
            progress.setStage(0);
            progress.setFirstTimeStartedAt(LocalDateTime.now());
            progress.setNumberOfTimesCompleted(0);
            LOGGER.info("New course progress for user " + user.getId() + " and course "
                    + courseName + " has been set up. Saving...");
            courseProgressRepository.save(progress);
            LOGGER.info("Course progress saved.");
        }
        current(progress);
    }

    @Override
    public void next(@NonNull UserEntity user, @NonNull String courseName) {
        final CourseProgress progress = courseProgressRepository.findByUserIdAndCourseName(
                user.getId(), courseName).orElseThrow(() -> new EntityNotFoundException(
                "Course progress for course " + courseName + " and user " + user.getId()
                + " does not exist", localizationLoader.getLocalizationForUser(
                ERROR_COURSE_PROGRESS_NOT_FOUND, user)));
        checkCourseIsNotUnderMaintenance(progress.getCourse(), user);
        LOGGER.info("Current stage in course " + courseName + " for user " + user.getId() + " is "
                + progress.getStage() + ". Incrementing by 1...");
        progress.setStage(progress.getStage() + 1);
        courseProgressRepository.save(progress);
        LOGGER.debug("Course stage incremented and progress saved.");

        if (progress.getStage().equals(progress.getCourse().getAmountOfLessons())) {
            LOGGER.info("User " + user.getId() + " has completed course " + courseName
                    + ". Commencing ending sequence...");
            end(user, progress);
            return;
        }
        final Lesson lesson = lessonRepository.findByPositionAndCourseName(
                progress.getStage(), progress.getCourse().getName()).get();

        if (lesson.getDelay() > 0) {
            LOGGER.debug("Lesson " + lesson.getId() + " has a delay of " + lesson.getDelay()
                    + " minutes. Creating a new lesson timed trigger...");
            final TimedTrigger trigger = timingService.createTrigger(progress);
            LOGGER.debug("New trigger " + trigger.getId() + " for user " + user.getId()
                    + " and lesson " + lesson.getId() + " has been created. It will activate at "
                    + trigger.getTarget() + ".");
        }
        current(progress);
    }

    @Override
    public void current(@NonNull CourseProgress courseProgress) {
        final Course course = courseProgress.getCourse();
        final UserEntity user = courseProgress.getUser();
        checkCourseIsNotUnderMaintenance(course, user);

        final Lesson lesson = lessonRepository.findByPositionAndCourseName(
                courseProgress.getStage(), course.getName()).get();
        checkLessonHasContent(lesson, user);

        final Optional<LessonTrigger> potentialTrigger = timingService
                .findLessonTrigger(user, courseProgress);

        if (potentialTrigger.isPresent()) {
            LOGGER.debug("User " + user.getId() + " is currently awaiting lesson "
                    + lesson.getId() + " in course " + course.getName()
                    + ". Sending error message...");
            clientManager.getClient(course.getBot()).sendMessage(user,
                    localizationLoader.getLocalizationForUser(ERROR_AWAITING_LESSON, user,
                    PARAM_TIME_LEFT, textUtil.formatTimeLeft(user, localizationLoader,
                    timingService.getTimeLeft(potentialTrigger.get()))));
            LOGGER.debug("Message sent.");
            return;
        }
        
        LOGGER.debug("Sending content for lesson " + lesson.getId() + " to user "
                + user.getId() + "...");
        for (int i = 0; i < lesson.getStructure().size() - 1; i++) {
            contentService.sendLocalizedContent(contentService.getMappingById(
                    lesson.getStructure().get(i).getId(), user), user, course.getBot());
        }
        LOGGER.debug("All except last content has been sent.");
        final List<Message> lastContent = contentService.sendLocalizedContent(
                contentService.getMappingById(lesson.getStructure().get(lesson.getStructure()
                .size() - 1).getId(), user), user, course.getBot());
        LOGGER.debug("Last content has been sent.");
        if (course.isHomeworkIncluded() && lesson.isHomeworkIncluded()) {
            LOGGER.debug("Lesson " + lesson.getId() + " includes homework. "
                    + "Commencing homework sequence...");
            homeworkService.initiateHomework(user, homeworkService.getHomework(
                    lesson.getHomework().getId(), courseProgress.getUser(), course.getBot()));
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
        LOGGER.debug("This is not the last lesson. Checking if the next lesson has a delay...");
        final Lesson nextLesson = lessonRepository.findByPositionAndCourseName(
                courseProgress.getStage() + 1, course.getName()).get();
        if (nextLesson.getDelay() > 0) {
            LOGGER.debug("Lesson " + nextLesson.getId() + " has a delay. "
                    + "No next lesson button will be created.");
            next(user, course.getName());
            return;
        }

        LOGGER.debug("Next lesson does not have a delay. "
                + "Initializing next lesson menu button...");
        final Message menuMessage;
        if (lastContent.size() > 1) {
            LOGGER.warn("Last content in lesson " + lesson.getId() + " is a media group. "
                    + "It is a recomendation to avoid such cases since it requires an "
                    + "additional message to be sent for menu.");
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .getLocalizationForUser(SERVICE_COURSE_NEXT_STAGE_MEDIA_GROUP_BYPASS, user);
            menuMessage = clientManager.getClient(course.getBot())
                    .sendMessage(user, mediaGroupBypassMessageLoc);
            LOGGER.debug("Additional message for menu has been sent.");
        } else {
            LOGGER.debug("Last message in lesson " + lesson.getId() + " is not a media group. "
                    + "Menu will be attached to it.");    
            menuMessage = lastContent.get(0);
        }
        menuService.initiateMenu(NEXT_STAGE_MENU, user, course.getName()
                + COURSE_NAME_LESSON_INDEX_DIVIDER + courseProgress.getStage(),
                menuMessage.getMessageId(), course.getBot());
        menuService.addToMenuTerminationGroup(user, user, course.getBot(),
                menuMessage.getMessageId(),
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
        clientManager.getClient(courseProgress.getCourse().getBot())
                .sendMessage(user, localization);
        if (!reviewService.isBasicReviewForCourseAndUserAvailable(user,
                courseProgress.getCourse())) {
            reviewService.initiateBasicReview(user, courseProgress.getCourse());
        }
    }

    @Override
    @NonNull
    public Course getCourseByName(@NonNull String courseName, @NonNull UserEntity user,
            @NonNull Bot bot) {
        final Course course = courseRepository.findByName(courseName).orElseThrow(() ->
                new EntityNotFoundException("Course " + courseName + " does not exist",
                localizationLoader.getLocalizationForUser(ERROR_COURSE_NOT_FOUND, user)));
        if (!course.getBot().equals(bot) && !botService.getBotFather().equals(bot)) {
            throw new AccessDeniedException("Course " + courseName + " is not available for bot "
                    + bot.getName(), localizationLoader.getLocalizationForUser(
                    ERROR_BOT_VISIBILITY_MISMATCH, user));
        }
        return course;
    }

    @Override
    @NonNull
    public Course getCourseById(@NonNull Long id, @NonNull UserEntity user, @NonNull Bot bot) {
        final Course course = courseRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Course " + id + " does not exist",
                localizationLoader.getLocalizationForUser(ERROR_COURSE_NOT_FOUND, user)));
        if (!course.getBot().equals(bot)) {
            throw new AccessDeniedException("Course " + id + " is not available for bot "
                    + bot.getName(), localizationLoader.getLocalizationForUser(
                    ERROR_BOT_VISIBILITY_MISMATCH, user));
        }
        return course;
    }

    @Override
    @NonNull
    public List<Course> getByBot(@NonNull Bot bot) {
        return courseRepository.findByBot(bot);
    }

    @Override
    @NonNull
    public List<Course> getAllOwnedByUser(@NonNull UserEntity user, @NonNull Bot bot) {
        final List<PaymentDetails> paymentDetails = paymentService.getAllForUserAndBot(user, bot);

        return paymentDetails.stream().map(pd -> pd.getCourse()).toList();
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
        final UserEntity user = userService.getUser(userId, userService.getDiretor());
        return courseProgressRepository.findByUserIdAndCourseName(userId, courseName)
                .orElseThrow(() -> new EntityNotFoundException("Course progress for user "
                + userId + " and course " + courseName, localizationLoader.getLocalizationForUser(
                ERROR_COURSE_PROGRESS_NOT_FOUND, user)));
    }

    @Override
    @NonNull
    public CourseProgress getProgress(@NonNull Long id, @NonNull UserEntity user) {
        return courseProgressRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Course progress with id " + id, localizationLoader
                .getLocalizationForUser(ERROR_COURSE_PROGRESS_NOT_FOUND, user)));
    }
    
    @Override
    public void delete(@NonNull Course course, @NonNull UserEntity user) {
        if (!isDeletable(course)) {
            throw new ForbiddenOperationException("Cannot delete a course which has been "
                    + "bought by users before", localizationLoader.getLocalizationForUser(
                    ERROR_CANNOT_DELETE_BOUGHT_COURSE, user));
        }
        courseRepository.delete(course);
    }

    @Override
    @NonNull
    public Course createInitialCourse(@NonNull Bot bot) {
        try {
            return getCourseByName(TEST_COURSE_NAME, userService.getDiretor(), bot);
        } catch (EntityNotFoundException e) {
            LOGGER.info("Test course does not exist. Creating...");
            return createCourse(bot, TEST_COURSE_NAME, TEST_COURSE_PRICE,
                    TEST_COURSE_AMOUNT_OF_LESSONS);
        }
    }

    @Override
    @NonNull
    public Course createCourse(@NonNull Bot bot, @NonNull String courseName,
            @NonNull Integer price, @NonNull Integer amountOfLessons) {
        final Course course = new Course();
    
        course.setName(courseName);
        course.setPrice(price);
        course.setAmountOfLessons(amountOfLessons);
        course.setRefundStage(-1);
        course.setBot(bot);
        course.setUnderMaintenance(true);
        final List<Lesson> lessons = getLessons(course);
        course.setLessons(lessons);
        course.setFeedbackIncluded(true);
        course.setHomeworkIncluded(true);
        
        LOGGER.debug("Persisting the course...");
        save(course);
        lessonRepository.saveAll(lessons);
        return course;
    }

    @Override
    public boolean isDeletable(@NonNull Course course) {
        return paymentService.getAllForCourse(course, PageRequest.of(0, 1)).isEmpty();
    }

    @Override
    public void checkCourseIsNotUnderMaintenance(@NonNull Course course,
            @NonNull UserEntity user) {
        if (course.isUnderMaintenance()) {
            throw new OnMaintenanceException("Course " + course.getName() + " is currently "
                    + "marked as under maintenance", localizationLoader.getLocalizationForUser(
                    ERROR_COURSE_UNDER_MAINTENANCE, user));
        }
    }

    private List<Lesson> getLessons(Course course) {
        final List<Lesson> lessons = new ArrayList<>();

        for (int i = 0; i < course.getAmountOfLessons(); i++) {
            final Lesson lesson = new Lesson();
            lesson.setCourse(course);
            lesson.setPosition(i);
            lesson.setDelay(0);
            lesson.setStructure(List.of());
            lessons.add(lesson);
        }
        return lessons;
    }

    private void checkLessonHasContent(Lesson lesson, UserEntity user) {
        if (lesson.getStructure().isEmpty()) {
            throw new ForbiddenOperationException("Lesson " + lesson.getId() + " does not have "
                    + "any content", localizationLoader.getLocalizationForUser(
                    ERROR_NO_CONTENT_IN_LESSON, user));
        }
    }
}
