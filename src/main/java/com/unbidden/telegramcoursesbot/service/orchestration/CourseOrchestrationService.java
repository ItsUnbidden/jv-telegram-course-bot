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
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.dto.internal.UsersByCourseStageCountDto;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto.Result;
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
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseInvoice.PaymentType;
import com.unbidden.telegramcoursesbot.repository.ReviewRepository;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.LessonTrigger;
import com.unbidden.telegramcoursesbot.model.TelegramInvoice;
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

    public CourseResponseDto getById(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(botRole, courseId);

        return mapper.toDto(course, contentService.getLocalizedText(botRole, course.getTitle()));
    }

    public List<CourseResponseDto> getByBot(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return courseService.getByBot(botRole.getBot()).stream()
                .map(c -> mapper.toDto(c, contentService.getLocalizedText(botRole, c.getTitle())))
                .toList();
    }

    public List<CourseResponseDto> getAllOwnedByUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return courseService.getAllOwnedByUser(botRole).stream()
                .map(c -> mapper.toDto(c, contentService.getLocalizedText(botRole, c.getTitle())))
                .toList();
    }

    public List<CourseResponseDto> getAllAvailableByUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");
        
        return courseService.getAllAvailableByUser(botRole).stream()
                .map(c -> mapper.toDto(c, contentService.getLocalizedText(botRole, c.getTitle())))
                .toList();
    }

    public List<CourseMenuDto> getCourseMenuDtosForOwnedCourses(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return courseService.getCourseMenuDtosForOwnedCourses(botRole);
    }

    public CourseMenuDto getCourseMenuDtoForCourse(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return courseService.getCourseMenuDtoForCourse(botRole, courseId);
    }

    public List<UsersByCourseStageCountDto> countAndGroupByCourseStage(Long courseId) {
        Assert.notNull(courseId, "courseId cannot be null");

        return courseService.countAndGroupByCourseStage(courseId);
    }

    public void checkDeletable(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        courseService.checkDeletable(botRole, courseId);
    }

    public void checkCourseIsNotUnderMaintenance(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        courseService.checkCourseIsNotUnderMaintenance(botRole, courseId);
    }

    public void initCourse(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        courseService.checkCourseIsNotUnderMaintenance(botRole, courseId);
        if (!checkWhetherCourseIsAvailable(botRole, courseId)) return;

        current(botRole, courseService.createOrLoadProgress(botRole, courseId));
    }

    public void current(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        current(botRole, entityUtil.getCourseProgressForUser(botRole, courseId));
    }

    public void next(BotRole botRole, Long courseId, Long currentLessonId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(currentLessonId, "currentLessonId cannot be null");

        courseService.checkCourseIsNotUnderMaintenance(botRole, courseId);
        if (!checkWhetherCourseIsAvailable(botRole, courseId)) return;

        final CourseProgress progress = courseService.incrementStage(botRole, courseId, currentLessonId);
        LOGGER.debug("Course stage incremented and progress saved.");

        menuService.terminateMenuGroup(MenuTerminationGroupKey.COURSE_NEXT_STAGE, progress.getId());

        if (progress.getStage() >= progress.getCourse().getLessons().size()) {
            LOGGER.info("User " + botRole.getUser().getId() + " has completed course " + courseId
                    + ". Commencing ending sequence...");
            end(botRole, progress);
            return;
        }
        
        timingService.createLessonTriggerIfNeeded(botRole, courseId);

        current(botRole, progress);
    }

    public void createCourse(BotRole botRole, Long titleContentId, String languageCode,
            PaymentType paymentType, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(titleContentId, "titleContentId cannot be null");
        Assert.notNull(paymentType, "paymentType cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        LOGGER.info("User " + botRole.getUser().getId() + " is trying to create a new course.");  

        final Course course = courseService.createCourse(botRole, titleContentId, paymentType, languageCode, messages);

        LOGGER.info("A new course " + course.getId() + " has been created.");
        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.NEW_COURSE_CREATED, botRole));
        LOGGER.debug("Message sent.");
    }

    public void selectStage(BotRole botRole, Long courseId, int stage) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.state(stage >= 0, "stage must be greater than 0");

        CourseProgress progress = entityUtil.getCourseProgressForUser(botRole, courseId);

        courseService.checkCourseIsNotUnderMaintenance(botRole, courseId);
        if (!checkWhetherCourseIsAvailable(botRole, courseId)) return;

        if (progress.getNumberOfTimesCompleted() < 1) {
            throw new ForbiddenOperationException("User " + botRole.getUser().getId() + " must complete "
                    + "course " + courseId + " before they can choose lessons",
                    localizationLoader.localize(Error.SELECT_LESSON_COURSE_NOT_COMPLETED, botRole));
        }
        
        progress = courseService.setCourseProgressStage(botRole, courseId, stage);
        current(botRole, progress);
    }

    public void toggleMaintenance(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = courseService.toggleMaintenance(botRole, courseId);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.COURSE_MAINTENANCE_TOGGLE_SUCCESS, botRole,
                new Localizations.Service.CourseMaintenanceToggleSuccessParams(getStatus(botRole, course.isUnderMaintenance()),
                    contentService.getLocalizedText(botRole, course.getTitle()))));
        LOGGER.debug("Message sent.");
    }

    public void toggleFeedbackInclusion(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = courseService.toggleFeedbackInclusion(botRole, courseId);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.COURSE_FEEDBACK_UPDATE_SUCCESS, botRole,
                new Localizations.Service.CourseFeedbackUpdateSuccessParams(getStatus(botRole, course.isFeedbackIncluded()),
                    contentService.getLocalizedText(botRole, course.getTitle()))));
        LOGGER.debug("Message sent.");
    }

    public void toggleHomeworkInclusion(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = courseService.toggleHomeworkInclusion(botRole, courseId);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.COURSE_HOMEWORK_UPDATE_SUCCESS, botRole,
                new Localizations.Service.CourseHomeworkUpdateSuccessParams(getStatus(botRole, course.isHomeworkIncluded()),
                    contentService.getLocalizedText(botRole, course.getTitle()))));
        LOGGER.debug("Message sent.");
    }

    public void updateCoursePrice(BotRole botRole, Long courseId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);
        
        final Integer newPrice = validatorUtil.parseIntInBounds(botRole, messages.getFirst(), 1, PaymentOrchestrationService.MAX_PRICE);
        
        final Course course = courseService.updateCoursePrice(botRole, courseId, newPrice);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.COURSE_PRICE_UPDATE_SUCCESS, botRole,
                new Localizations.Service.CoursePriceUpdateSuccessParams(contentService.getLocalizedText(
                    botRole, course.getTitle()), ((TelegramInvoice)course.getInvoice()).getPrice())));
        LOGGER.debug("Message sent.");
    }

    public void deleteCourse(BotRole botRole, Long courseId, String confirmationPhrase, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(confirmationPhrase, "confirmationPhrase cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        LOGGER.info("User " + botRole.getUser().getId() + " is trying to delete course " + courseId + ".");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);
        final String providedStr = validatorUtil.checkText(botRole, messages.getFirst());

        LOGGER.debug("User has provided this string - " + providedStr + ". Checking if this matches the confirmation phrase...");
        if (!confirmationPhrase.equals(providedStr)) {
            throw new InvalidDataSentException("Provided string does not match the confirmation phrase",
                    localizationLoader.localize(Localizations.Error.DELETE_COURSE_CONFIRMATION_PHRASE_FAILURE, botRole));
        }
        LOGGER.debug("Confirmation phrase matches. Deleting course " + courseId + "...");

        courseService.deleteCourse(botRole, courseId);
        LOGGER.info("Course " + courseId + " has been deleted.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.DELETE_COURSE_SUCCESS, botRole));
        LOGGER.debug("Message sent.");
    }

    public void updateRefundStage(BotRole botRole, Long courseId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");
        
        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);
        
        final Course course = courseService.updateRefundStage(botRole, courseId, validatorUtil.parseInt(botRole, messages.getFirst()));
        final TelegramInvoice invoice = (TelegramInvoice)course.getInvoice();

        LOGGER.info("Refund stage for course " + courseId  + " has been updated.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.NEW_REFUND_STAGE_SUCCESS, botRole,
                    new Localizations.Service.NewRefundStageSuccessParams(
                        contentService.getLocalizedText(botRole, course.getTitle()),
                        invoice.getRefundStage() == null
                            ? localizationLoader.localize(Localizations.Service.NOT_AVAILABLE, botRole).getData()
                            : invoice.getRefundStage().toString()
                    )));
        LOGGER.debug("Message sent.");
    }

    private String getStatus(BotRole botRole, boolean status) {
        return status ? localizationLoader.localize(Localizations.Service.STATUS_ENABLED, botRole).getData()
                : localizationLoader.localize(Localizations.Service.STATUS_DISABLED, botRole).getData();
    }

    private void current(BotRole botRole, CourseProgress progress) {
        Assert.notNull(progress, "Course progress cannot be null");

        final Course course = progress.getCourse();

        courseService.checkCourseIsNotUnderMaintenance(botRole, course.getId());
        if (!checkWhetherCourseIsAvailable(botRole, course.getId())) return;
        if (checkForActiveLessonTrigger(botRole, progress)) return;

        final Lesson lesson = entityUtil.getLessonByPositionAndCourseId(botRole,
                progress.getStage(), course.getId());
        final List<SendMessageResultDto> lastContent = sendLessonContent(botRole, lesson, course);

        if (course.isHomeworkIncluded() && lesson.isHomeworkIncluded()) {
            LOGGER.debug("Lesson " + lesson.getId() + " includes homework. "
                    + "Commencing homework sequence...");
            homeworkService.initHomework(botRole, lesson.getHomework().getId());
            return;
        }
        
        LOGGER.debug("Lesson " + lesson.getId() + " or the course does not have any homework."
                + " Checking, if this is the last lesson...");
        if (progress.getStage() >= course.getLessons().size() - 1) {
            next(botRole, course.getId(), lesson.getId());
            return;
        }

        LOGGER.debug("This is not the last lesson. Checking if the next lesson has a delay...");
        final Lesson nextLesson = entityUtil.getLessonByPositionAndCourseId(botRole,
                progress.getStage() + 1, course.getId());

        if (nextLesson.getDelay() > 0 && progress.getNumberOfTimesCompleted() == 0) {
            LOGGER.debug("Lesson " + nextLesson.getId() + " has a delay. "
                    + "No next lesson button will be created.");
            next(botRole, course.getId(), lesson.getId());
            return;
        }

        LOGGER.debug("Next lesson does not have a delay. "
                + "Initializing next lesson menu button...");
        sendNextLessonMenu(botRole, lesson, lastContent, progress);
    }

    private void end(BotRole botRole, CourseProgress courseProgress) {
        if (courseProgress.getNumberOfTimesCompleted() > 0) {
            LOGGER.info("User " + botRole.getUser().getId() + " has completed course "
                    + courseProgress.getCourse().getId() + " for the "
                    + (courseProgress.getNumberOfTimesCompleted() + 1) + " time!");
        } else {
            LOGGER.info("User " + botRole.getUser().getId() + " has completed course "
                    + courseProgress.getCourse().getId() + " for the first time!");
        }
        courseProgress = courseService.resetCourseProgress(botRole, courseProgress.getCourse().getId());

        if (courseProgress.getCourse().getEndMapping() != null) {
            contentService.sendLocalizedContent(botRole, courseProgress.getCourse().getEndMapping().getId());
        } else {
            clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.COURSE_COMPLETED_DEFAULT, botRole,
                    new Localizations.Service.CourseCompletedDefaultParams(
                        contentService.getLocalizedText(botRole, courseProgress.getCourse().getTitle().getId())
                    )
            ));
        }

        if (!reviewRepository.existsByCourseIdAndUserId(courseProgress.getCourse().getId(), courseProgress.getUser().getId())) {
            reviewService.initiateBasicReview(botRole, courseProgress.getCourse().getId());
        }
    }

    private boolean checkForActiveLessonTrigger(BotRole botRole, CourseProgress progress) {
        final Optional<LessonTrigger> potentialTrigger = timingService
                .findLessonTrigger(botRole.getId(), progress.getCourse().getId(), progress.getStage());

        if (potentialTrigger.isPresent()) {
            LOGGER.debug("User " + progress.getUser().getFullName() + " is currently awaiting lesson number "
                    + progress.getStage() + " in course " + progress.getCourse().getId()
                    + ". Sending error message...");
            clientManager.sendMessage(botRole, localizationLoader.localize(Error.AWAITING_LESSON, botRole,
                    new Error.AwaitingLessonParams(textUtil.formatTimeLeft(botRole, localizationLoader,
                        timingService.getTimeLeft(potentialTrigger.get())))));
            LOGGER.debug("Message sent.");
            return true;
        }
        return false;
    }

    private void checkLessonHasContent(BotRole botRole, Lesson lesson) {
        if (lesson.getStructure().isEmpty()) {
            throw new ForbiddenOperationException("Lesson " + lesson.getId() + " does not have "
                    + "any content", localizationLoader.localize(
                    Error.NO_CONTENT_IN_LESSON, botRole));
        }
    }

    private List<SendMessageResultDto> sendLessonContent(BotRole botRole, Lesson lesson, Course course) {
        checkLessonHasContent(botRole, lesson);
        LOGGER.debug("Sending content for lesson " + lesson.getId() + " to user "
                + botRole.getUser().getId() + "...");
        for (int i = 0; i < lesson.getStructure().size() - 1; i++) {
            contentService.sendLocalizedContent(botRole, lesson.getStructure().get(i).getId());
        }
        if (lesson.getStructure().size() > 1) LOGGER.debug("All except last content has been sent.");

        final List<SendMessageResultDto> lastContent = contentService.sendLocalizedContent(botRole,
                lesson.getStructure().getLast().getId());
        LOGGER.debug("Last content has been sent.");

        return lastContent;
    }

    private void sendNextLessonMenu(BotRole botRole, Lesson lesson, List<SendMessageResultDto> lastContent,
                CourseProgress progress) {
        final SendMessageResultDto menuMessage;
        if (lastContent.size() > 1) {
            LOGGER.debug("The last content in lesson " + lesson.getId() + " is a media group. "
                    + "It is a recomendation to avoid such cases since it requires an "
                    + "additional message to be sent for the menu.");
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .localize(Localizations.Service.COURSE_NEXT_STAGE_MEDIA_GROUP_BYPASS, botRole);

            menuMessage = clientManager.sendMessage(botRole, mediaGroupBypassMessageLoc);
            LOGGER.debug("Additional message for the menu has been sent.");
        } else {
            LOGGER.debug("The last message in lesson " + lesson.getId() + " is not a media group. "
                    + "The menu will be attached to it.");    
            menuMessage = lastContent.get(0);
        }

        if (menuMessage.getResult() == Result.OK) {
            menuService.initiateMenu(botRole, MenuKey.COURSE_NEXT_STAGE, LESSON_ID_PARAM, lesson.getId().toString(),
                    menuMessage.getMessage().getMessageId(), MenuTerminationGroupKey.COURSE_NEXT_STAGE, progress.getId());
            LOGGER.debug("Next lesson menu sent.");
        } else {
            LOGGER.error("Failed to send the next lesson menu to user "
                    + botRole.getUser().getId() + " in bot " + botRole.getBot().getId() + ".");
            // TODO: potentially introduce a fallback
        }
    }

    private boolean checkWhetherCourseIsAvailable(BotRole botRole, Long courseId) {
        if (!paymentService.isAvailable(botRole.getUser(), courseId)) {
            paymentService.sendInvoice(botRole, courseId);
            return false;
        }
        LOGGER.debug("Course " + courseId + " is available for user " + botRole.getUser().getFullName() + ".");
        return true;
    }
}
