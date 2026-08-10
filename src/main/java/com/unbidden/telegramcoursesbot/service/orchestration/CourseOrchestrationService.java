package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.LessonTrigger;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.MenuTerminationGroupKey;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.TextUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseOrchestrationService {
    private static final Logger LOGGER = LogManager.getLogger(CourseOrchestrationService.class);

    public static final String COURSE_NAME_LESSON_INDEX_DIVIDER = "/";
    public static final String COURSE_NEXT_STAGE_MENU_TERMINATION = "course_progress_%s_next_stage";

    private final CourseService courseService;

    private final HomeworkOrchestrationService homeworkService;

    private final PaymentOrchestrationService paymentService;

    private final TimingService timingService;

    private final ContentService contentService;

    private final MenuService menuService;

    private final ReviewOrchestrationService reviewService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final TextUtil textUtil;

    private final EntityUtil entityUtil;

    public void initCourse(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        courseService.checkCourseIsNotUnderMaintenance(user, courseId);
        if (!checkWhetherCourseIsAvailable(user, bot, courseId)) return;

        current(courseService.createOrLoadProgress(user, bot, courseId));
    }

    public void current(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        current(entityUtil.getCourseProgressForUser(user, bot, courseId));
    }

    public void next(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        courseService.checkCourseIsNotUnderMaintenance(user, courseId);
        if (!checkWhetherCourseIsAvailable(user, bot, courseId)) return;

        final CourseProgress progress = courseService.incrementStage(user, bot, courseId);
        LOGGER.debug("Course stage incremented and progress saved.");

        if (progress.getStage() >= progress.getCourse().getNumberOfLessons()) {
            LOGGER.info("User " + user.getId() + " has completed course " + courseId
                    + ". Commencing ending sequence...");
            end(user, progress);
            return;
        }
        
        timingService.createLessonTriggerIfNeeded(user, bot, courseId);

        current(progress);
    }

    public void selectStage(UserEntity user, Bot bot, Long courseId, int stage) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.state(stage >= 0, "stage must be greater than 0");

        final CourseProgress progress = entityUtil.getCourseProgressForUser(user, bot, courseId);

        courseService.checkCourseIsNotUnderMaintenance(user, courseId);
        if (!checkWhetherCourseIsAvailable(user, progress.getCourse().getBot(), courseId)) return;

        if (progress.getNumberOfTimesCompleted() < 1) {
            throw new ForbiddenOperationException("User " + user.getId() + " must complete "
                    + "course " + courseId + " before they can choose lessons",
                    localizationLoader.getLocalizationForUser(
                    Error.SELECT_LESSON_COURSE_NOT_COMPLETED, user));
        }
        
        courseService.setCourseProgressStage(user, bot, courseId, stage);
        current(progress);
    }

    private void current(CourseProgress progress) {
        Assert.notNull(progress, "Course progress cannot be null");

        final Course course = progress.getCourse();
        final UserEntity user = progress.getUser();

        courseService.checkCourseIsNotUnderMaintenance(user, course.getId());
        if (!checkWhetherCourseIsAvailable(user, course.getBot(), course.getId())) return;
        if (checkForActiveLessonTrigger(progress)) return;

        final Lesson lesson = entityUtil.getLessonByPositionAndCourseId(
                user, course.getBot(), progress.getStage(), course.getId());
        final List<Message> lastContent = sendLessonContent(lesson, course, user);

        if (course.isHomeworkIncluded() && lesson.isHomeworkIncluded()) {
            LOGGER.debug("Lesson " + lesson.getId() + " includes homework. "
                    + "Commencing homework sequence...");
            homeworkService.initHomework(user, course.getBot(), lesson.getHomework().getId());
            return;
        }
        
        LOGGER.debug("Lesson " + lesson.getId() + " or the course does not have any homework."
                + " Checking, if this is the last lesson...");
        if (progress.getStage() >= course.getNumberOfLessons() - 1) {
            next(user, course.getBot(), course.getId());
            return;
        }

        LOGGER.debug("This is not the last lesson. Checking if the next lesson has a delay...");
        final Lesson nextLesson = entityUtil.getLessonByPositionAndCourseId(user, course.getBot(),
                progress.getStage() + 1, course.getId());

        if (nextLesson.getDelay() > 0 && progress.getNumberOfTimesCompleted() == 0) {
            LOGGER.debug("Lesson " + nextLesson.getId() + " has a delay. "
                    + "No next lesson button will be created.");
            next(user, course.getBot(), course.getId());
            return;
        }

        LOGGER.debug("Next lesson does not have a delay. "
                + "Initializing next lesson menu button...");
        sendNextLessonMenu(nextLesson, lastContent, progress);
    }

    private void end(UserEntity user, CourseProgress courseProgress) {
        if (courseProgress.getNumberOfTimesCompleted() > 0) {
            LOGGER.info("User " + user.getId() + " has completed course "
                    + courseProgress.getCourse().getId() + " for the "
                    + (courseProgress.getNumberOfTimesCompleted() + 1) + " time!");
        } else {
            LOGGER.info("User " + user.getId() + " has completed course "
                    + courseProgress.getCourse().getId() + " for the first time!");
        }
        courseProgress = courseService.resetCourseProgress(courseProgress.getUser(),
                courseProgress.getCourse().getBot(), courseProgress.getCourse().getId());

        if (courseProgress.getCourse().getEndMapping() != null) {
            contentService.sendLocalizedContent(user, courseProgress.getCourse().getBot(),
                    courseProgress.getCourse().getEndMapping().getId());
        }

        reviewService.initiateBasicReview(user, courseProgress.getCourse().getBot(), courseProgress.getCourse().getId());
    }

    private boolean checkForActiveLessonTrigger(CourseProgress progress) {
        final Optional<LessonTrigger> potentialTrigger = timingService
                .findLessonTrigger(progress.getUser().getId(),
                progress.getCourse().getId(), progress.getStage());

        if (potentialTrigger.isPresent()) {
            LOGGER.debug("User " + progress.getUser().getFullName() + " is currently awaiting lesson number "
                    + progress.getStage() + " in course " + progress.getCourse().getId()
                    + ". Sending error message...");
            clientManager.getClient(progress.getCourse().getBot()).sendMessage(progress.getUser(),
                    localizationLoader.getLocalizationForUser(Error.AWAITING_LESSON, progress.getUser(),
                    new Error.AwaitingLessonParams(textUtil.formatTimeLeft(progress.getUser(), localizationLoader,
                        timingService.getTimeLeft(potentialTrigger.get())))));
            LOGGER.debug("Message sent.");
            return true;
        }
        return false;
    }

    private void checkLessonHasContent(Lesson lesson, UserEntity user) {
        if (lesson.getStructure().isEmpty()) {
            throw new ForbiddenOperationException("Lesson " + lesson.getId() + " does not have "
                    + "any content", localizationLoader.getLocalizationForUser(
                    Error.NO_CONTENT_IN_LESSON, user));
        }
    }

    private List<Message> sendLessonContent(Lesson lesson, Course course, UserEntity user) {
        checkLessonHasContent(lesson, user);
        LOGGER.debug("Sending content for lesson " + lesson.getId() + " to user "
                + user.getId() + "...");
        for (int i = 0; i < lesson.getStructure().size() - 1; i++) {
            contentService.sendLocalizedContent(user, course.getBot(), lesson.getStructure().get(i).getId());
        }
        if (lesson.getStructure().size() > 1) LOGGER.debug("All except last content has been sent.");

        final List<Message> lastContent = contentService.sendLocalizedContent(user, course.getBot(),
                lesson.getStructure().getLast().getId());
        LOGGER.debug("Last content has been sent.");

        return lastContent;
    }

    private void sendNextLessonMenu(Lesson lesson, List<Message> lastContent, CourseProgress progress) {
        final Message menuMessage;
        if (lastContent.size() > 1) {
            LOGGER.debug("The last content in lesson " + lesson.getId() + " is a media group. "
                    + "It is a recomendation to avoid such cases since it requires an "
                    + "additional message to be sent for the menu.");
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .getLocalizationForUser(Localizations.Service.COURSE_NEXT_STAGE_MEDIA_GROUP_BYPASS, progress.getUser());

            menuMessage = clientManager.getClient(progress.getCourse().getBot())
                    .sendMessage(progress.getUser(), mediaGroupBypassMessageLoc);
            LOGGER.debug("Additional message for the menu has been sent.");
        } else {
            LOGGER.debug("The last message in lesson " + lesson.getId() + " is not a media group. "
                    + "The menu will be attached to it.");    
            menuMessage = lastContent.get(0);
        }

        menuService.initiateMenu(progress.getUser(), progress.getCourse().getBot(), MenuKey.COURSE_NEXT_STAGE,
                progress.getCourse().getId() + COURSE_NAME_LESSON_INDEX_DIVIDER + progress.getStage(),
                menuMessage.getMessageId());
        menuService.addToMenuTerminationGroup(progress.getUser(), progress.getUser(),
                progress.getCourse().getBot(), menuMessage.getMessageId(),
                MenuTerminationGroupKey.COURSE_NEXT_STAGE, progress.getId());
        LOGGER.debug("Next lesson menu sent.");
    }

    private boolean checkWhetherCourseIsAvailable(UserEntity user, Bot bot, Long courseId) {
        if (!paymentService.isAvailable(user, courseId)) {
            paymentService.sendInvoice(user, bot, courseId);
            return false;
        }
        LOGGER.debug("Course " + courseId + " is available for user " + user.getFullName() + ".");
        return true;
    }
}
