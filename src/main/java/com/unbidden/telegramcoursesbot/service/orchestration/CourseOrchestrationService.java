package com.unbidden.telegramcoursesbot.service.orchestration;

import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.CourseResponseDto;
import com.unbidden.telegramcoursesbot.dto.internal.CourseMenuDto;
import com.unbidden.telegramcoursesbot.dto.internal.UsersByCourseStageCountDto;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.mapper.CourseMapper;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.MenuTerminationGroupKey;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseInvoice.PaymentType;
import com.unbidden.telegramcoursesbot.repository.ReviewRepository;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.LessonTrigger;
import com.unbidden.telegramcoursesbot.model.TelegramInvoice;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.TextUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseOrchestrationService {
    private static final Logger LOGGER = LogManager.getLogger(CourseOrchestrationService.class);
    
    private static final String LESSON_ID_PARAM = "lessonId";

    private final ReviewRepository reviewRepository;

    private final CourseService courseService;

    private final HomeworkOrchestrationService homeworkService;

    private final PaymentOrchestrationService paymentService;

    private final TimingService timingService;

    private final ContentOrchestrationService contentService;

    private final MenuOrchestrationService menuService;

    private final ReviewOrchestrationService reviewService;

    private final CourseMapper mapper;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final TextUtil textUtil;

    private final EntityUtil entityUtil;

    private final ValidatorUtil validatorUtil;

    public CourseResponseDto getById(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(user, bot, courseId);

        return mapper.toDto(course, contentService.getLocalizedText(user, bot, course.getTitle()));
    }

    public List<CourseResponseDto> getByBot(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        return courseService.getByBot(bot).stream()
                .map(c -> mapper.toDto(c, contentService.getLocalizedText(user, bot, c.getTitle())))
                .toList();
    }

    public List<CourseResponseDto> getAllOwnedByUser(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        return courseService.getAllOwnedByUser(user, bot).stream()
                .map(c -> mapper.toDto(c, contentService.getLocalizedText(user, bot, c.getTitle())))
                .toList();
    }

    public List<CourseResponseDto> getAllAvailableByUser(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        
        return courseService.getAllAvailableByUser(user, bot).stream()
                .map(c -> mapper.toDto(c, contentService.getLocalizedText(user, bot, c.getTitle())))
                .toList();
    }

    public List<CourseMenuDto> getCourseMenuDtosForOwnedCourses(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        return courseService.getCourseMenuDtosForOwnedCourses(user, bot);
    }

    public CourseMenuDto getCourseMenuDtoForCourse(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return courseService.getCourseMenuDtoForCourse(user, bot, courseId);
    }

    public List<UsersByCourseStageCountDto> countAndGroupByCourseStage(Long courseId) {
        return courseService.countAndGroupByCourseStage(courseId);
    }

    public void checkDeletable(UserEntity user, Long courseId) {
        courseService.checkDeletable(user, courseId);
    }

    public void checkCourseIsNotUnderMaintenance(UserEntity user, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        courseService.checkCourseIsNotUnderMaintenance(user, courseId);
    }

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

    public void next(UserEntity user, Bot bot, Long courseId, Long currentLessonId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(currentLessonId, "currentLessonId cannot be null");

        courseService.checkCourseIsNotUnderMaintenance(user, courseId);
        if (!checkWhetherCourseIsAvailable(user, bot, courseId)) return;

        final CourseProgress progress = courseService.incrementStage(user, bot, courseId, currentLessonId);
        LOGGER.debug("Course stage incremented and progress saved.");

        menuService.terminateMenuGroup(MenuTerminationGroupKey.COURSE_NEXT_STAGE, progress.getId());

        if (progress.getStage() >= progress.getCourse().getLessons().size()) {
            LOGGER.info("User " + user.getId() + " has completed course " + courseId
                    + ". Commencing ending sequence...");
            end(user, progress);
            return;
        }
        
        timingService.createLessonTriggerIfNeeded(user, bot, courseId);

        current(progress);
    }

    public void createCourse(UserEntity user, Bot bot, Long titleContentId, String languageCode,
            PaymentType paymentType, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(titleContentId, "titleContentId cannot be null");
        Assert.notNull(paymentType, "paymentType cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        LOGGER.info("User " + user.getId() + " is trying to create a new course.");  

        final Course course = courseService.createCourse(user, bot, titleContentId, paymentType, languageCode, messages);

        LOGGER.info("A new course " + course.getId() + " has been created.");
        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.NEW_COURSE_CREATED, user));
        LOGGER.debug("Message sent.");
    }

    public void selectStage(UserEntity user, Bot bot, Long courseId, int stage) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.state(stage >= 0, "stage must be greater than 0");

        CourseProgress progress = entityUtil.getCourseProgressForUser(user, bot, courseId);

        courseService.checkCourseIsNotUnderMaintenance(user, courseId);
        if (!checkWhetherCourseIsAvailable(user, progress.getCourse().getBot(), courseId)) return;

        if (progress.getNumberOfTimesCompleted() < 1) {
            throw new ForbiddenOperationException("User " + user.getId() + " must complete "
                    + "course " + courseId + " before they can choose lessons",
                    localizationLoader.localize(
                    Error.SELECT_LESSON_COURSE_NOT_COMPLETED, user));
        }
        
        progress = courseService.setCourseProgressStage(user, bot, courseId, stage);
        current(progress);
    }

    public void toggleMaintenance(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = courseService.toggleMaintenance(user, bot, courseId);

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.COURSE_MAINTENANCE_TOGGLE_SUCCESS, user,
                new Localizations.Service.CourseMaintenanceToggleSuccessParams(getStatus(user, course.isUnderMaintenance()),
                    contentService.getLocalizedText(user, bot, course.getTitle()))));
        LOGGER.debug("Message sent.");
    }

    public void toggleFeedbackInclusion(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = courseService.toggleFeedbackInclusion(user, bot, courseId);

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.COURSE_FEEDBACK_UPDATE_SUCCESS, user,
                new Localizations.Service.CourseFeedbackUpdateSuccessParams(getStatus(user, course.isFeedbackIncluded()),
                    contentService.getLocalizedText(user, bot, course.getTitle()))));
        LOGGER.debug("Message sent.");
    }

    public void toggleHomeworkInclusion(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = courseService.toggleHomeworkInclusion(user, bot, courseId);

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.COURSE_HOMEWORK_UPDATE_SUCCESS, user,
                new Localizations.Service.CourseHomeworkUpdateSuccessParams(getStatus(user, course.isHomeworkIncluded()),
                    contentService.getLocalizedText(user, bot, course.getTitle()))));
        LOGGER.debug("Message sent.");
    }

    public void updateCoursePrice(UserEntity user, Bot bot, Long courseId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        validatorUtil.checkExactExpectedMessages(user, messages, 1);
        
        final Integer newPrice = validatorUtil.parseIntInBounds(user, messages.getFirst(), 1, PaymentOrchestrationService.MAX_PRICE);
        
        final Course course = courseService.updateCoursePrice(user, bot, courseId, newPrice);

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.COURSE_PRICE_UPDATE_SUCCESS, user,
                new Localizations.Service.CoursePriceUpdateSuccessParams(contentService.getLocalizedText(
                    user, bot, course.getTitle()), ((TelegramInvoice)course.getInvoice()).getPrice())));
        LOGGER.debug("Message sent.");
    }

    public void deleteCourse(UserEntity user, Bot bot, Long courseId, String confirmationPhrase, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(confirmationPhrase, "confirmationPhrase cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        LOGGER.info("User " + user.getId() + " is trying to delete course " + courseId + ".");

        validatorUtil.checkExactExpectedMessages(user, messages, 1);
        final String providedStr = validatorUtil.checkText(user, messages.getFirst());

        LOGGER.debug("User has provided this string - " + providedStr + ". Checking if this matches the confirmation phrase...");
        if (!confirmationPhrase.equals(providedStr)) {
            throw new InvalidDataSentException("Provided string does not match the confirmation phrase",
                    localizationLoader.localize(Localizations.Error.DELETE_COURSE_CONFIRMATION_PHRASE_FAILURE, user));
        }
        LOGGER.debug("Confirmation phrase matches. Deleting course " + courseId + "...");

        courseService.deleteCourse(user, bot, courseId);
        LOGGER.info("Course " + courseId + " has been deleted.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .localize(Localizations.Service.DELETE_COURSE_SUCCESS, user));
        LOGGER.debug("Message sent.");
    }

    public void updateRefundStage(UserEntity user, Bot bot, Long courseId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");
        
        validatorUtil.checkExactExpectedMessages(user, messages, 1);
        
        final Course course = courseService.updateRefundStage(user, bot, courseId, validatorUtil.parseInt(user, messages.getFirst()));
        final TelegramInvoice invoice = (TelegramInvoice)course.getInvoice();

        LOGGER.info("Refund stage for course " + courseId  + " has been updated.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .localize(Localizations.Service.NEW_REFUND_STAGE_SUCCESS, user,
                    new Localizations.Service.NewRefundStageSuccessParams(
                        contentService.getLocalizedText(user, bot, course.getTitle()),
                        invoice.getRefundStage() == null
                            ? localizationLoader.localize(Localizations.Service.NOT_AVAILABLE, user).getData()
                            : invoice.getRefundStage().toString()
                    )));
        LOGGER.debug("Message sent.");
    }

    private String getStatus(UserEntity user, boolean status) {
        return status ? localizationLoader.localize(Localizations.Service.STATUS_ENABLED, user).getData()
                : localizationLoader.localize(Localizations.Service.STATUS_DISABLED, user).getData();
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
        if (progress.getStage() >= course.getLessons().size() - 1) {
            next(user, course.getBot(), course.getId(), lesson.getId());
            return;
        }

        LOGGER.debug("This is not the last lesson. Checking if the next lesson has a delay...");
        final Lesson nextLesson = entityUtil.getLessonByPositionAndCourseId(user, course.getBot(),
                progress.getStage() + 1, course.getId());

        if (nextLesson.getDelay() > 0 && progress.getNumberOfTimesCompleted() == 0) {
            LOGGER.debug("Lesson " + nextLesson.getId() + " has a delay. "
                    + "No next lesson button will be created.");
            next(user, course.getBot(), course.getId(), lesson.getId());
            return;
        }

        LOGGER.debug("Next lesson does not have a delay. "
                + "Initializing next lesson menu button...");
        sendNextLessonMenu(lesson, lastContent, progress);
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
        } else {
            clientManager.getClient(courseProgress.getCourse().getBot()).sendMessage(user,
                    localizationLoader.localize(Localizations.Service.COURSE_COMPLETED_DEFAULT, user,
                        new Localizations.Service.CourseCompletedDefaultParams(contentService.getLocalizedText(user,
                            courseProgress.getCourse().getBot(), courseProgress.getCourse().getTitle().getId()))));
        }

        if (!reviewRepository.existsByCourseIdAndUserId(courseProgress.getCourse().getId(), courseProgress.getUser().getId())) {
            reviewService.initiateBasicReview(user, courseProgress.getCourse().getBot(), courseProgress.getCourse().getId());
        }
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
                    localizationLoader.localize(Error.AWAITING_LESSON, progress.getUser(),
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
                    + "any content", localizationLoader.localize(
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
                    .localize(Localizations.Service.COURSE_NEXT_STAGE_MEDIA_GROUP_BYPASS, progress.getUser());

            menuMessage = clientManager.getClient(progress.getCourse().getBot())
                    .sendMessage(progress.getUser(), mediaGroupBypassMessageLoc);
            LOGGER.debug("Additional message for the menu has been sent.");
        } else {
            LOGGER.debug("The last message in lesson " + lesson.getId() + " is not a media group. "
                    + "The menu will be attached to it.");    
            menuMessage = lastContent.get(0);
        }

        menuService.initiateMenu(progress.getUser(), progress.getCourse().getBot(), MenuKey.COURSE_NEXT_STAGE,
                LESSON_ID_PARAM, lesson.getId().toString(), menuMessage.getMessageId(),
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
