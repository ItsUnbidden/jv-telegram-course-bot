package com.unbidden.telegramcoursesbot.service.orchestration;

import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.config.properties.PagedRequestProperties;
import com.unbidden.telegramcoursesbot.dto.HomeworkResponseDto;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto.Result;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.MediaTypeParseException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.mapper.HomeworkMapper;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.MenuTerminationGroupKey;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress.Status;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.impl.InMemoryHomeworkFeedbackSessionRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.model.HomeworkFeedbackSession;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

@Service
public class HomeworkOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(HomeworkOrchestrationService.class);

    private static final String PROGRESS_ID_PARAM = "progressId";
    private static final String LESSON_ID_PARAM = "lessonId";

    public static final int MAX_HOMEWORK_DELAY = 720;

    private final InMemoryHomeworkFeedbackSessionRepository feedbackSessionRepository;

    private final HomeworkService homeworkService;

    private final TimingService timingService;

    private final ContentOrchestrationService contentService;

    private final CourseOrchestrationService courseService;

    private final MenuOrchestrationService menuService;

    private final UserService userService;

    private final HomeworkMapper mapper;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    private final ValidatorUtil validatorUtil;

    private final PagedRequestProperties pagedRequestProperties;

    public HomeworkOrchestrationService(InMemoryHomeworkFeedbackSessionRepository feedbackSessionRepository,
            HomeworkService homeworkService, TimingService timingService,
            ContentOrchestrationService contentService, MenuOrchestrationService menuService, UserService userService,
            HomeworkMapper mapper, LocalizationLoader localizationLoader, ClientManager clientManager,
            EntityUtil entityUtil, @Lazy CourseOrchestrationService courseService, ValidatorUtil validatorUtil,
            PagedRequestProperties pagedRequestProperties) {
        this.feedbackSessionRepository = feedbackSessionRepository;
        this.homeworkService = homeworkService;
        this.timingService = timingService;
        this.contentService = contentService;
        this.menuService = menuService;
        this.userService = userService;
        this.mapper = mapper;
        this.localizationLoader = localizationLoader;
        this.clientManager = clientManager;
        this.entityUtil = entityUtil;
        this.courseService = courseService;
        this.validatorUtil = validatorUtil;
        this.pagedRequestProperties = pagedRequestProperties;
    }

    public HomeworkResponseDto getById(BotRole botRole, Long homeworkId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        return mapper.toDto(entityUtil.getHomeworkById(botRole, homeworkId));
    }

    public void initHomework(BotRole botRole, Long homeworkId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final HomeworkProgress progress = homeworkService.createOrLoadProgress(botRole, homeworkId);

        if (timingService.existsHomeworkTrigger(botRole.getId(), homeworkId)) {
            LOGGER.debug("User " + botRole.getUser().getId() + " is currently awaiting homework.");
            return;
        }
        if (progress.getHomework().getDelay() > 0 && timingService.createHomeworkTriggerIfNeeded(botRole, homeworkId).isPresent()) {
            return;
        }
        
        sendHomework(botRole, progress);
    }

    public void createHomework(BotRole botRole, Long lessonId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        LOGGER.info("User " + botRole.getUser().getId() + " want to add a new homework to lesson " + lessonId + ".");

        validatorUtil.checkAtLeastExpectedMessages(botRole, messages, 1);

        String languageCode = botRole.getUser().getLanguageCode();
        if (messages.size() > 1 && validatorUtil.checkLanguageCode(botRole, messages.getLast())) {
            languageCode = messages.getLast().getText();
            messages.removeLast();
        }

        final Homework homework = homeworkService.createHomework(botRole, lessonId, languageCode, messages);

        LOGGER.info("New Homework " + homework.getId() + " has been created for lesson " + lessonId + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.NEW_HOMEWORK_CREATED,
                botRole, new Localizations.Service.NewHomeworkCreatedParams(homework.getId())));
        LOGGER.debug("Message sent.");
    }

    public void updateDelay(BotRole botRole, Long homeworkId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);
        LOGGER.info("User " + botRole.getUser().getId() + " is trying to update delay for homework " + homeworkId + "...");
            
        final int newDelay = Math.clamp(validatorUtil.parseIntInBounds(botRole, messages.getFirst(),
                Integer.MIN_VALUE, MAX_HOMEWORK_DELAY), 0, MAX_HOMEWORK_DELAY);
        final Homework homework = homeworkService.updateDelay(botRole, homeworkId, newDelay);

        LOGGER.info("Homework " + homework.getId() + " now has a delay of " + homework.getDelay() + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.NEW_DELAY_SET_SUCCESS, botRole));
        LOGGER.debug("Message sent.");
    }

    public void updateContent(BotRole botRole, Long homeworkId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");
        
        validatorUtil.checkAtLeastExpectedMessages(botRole, messages, 2);
        LOGGER.info("User " + botRole.getUser().getId() + " is trying to update homework " + homeworkId + "...");  

        final String languageCode = validatorUtil.checkLanguageCode(botRole, messages.getLast())
                ? messages.getLast().getText().trim()
                : botRole.getUser().getLanguageCode();

        final Homework homework = homeworkService.updateContent(botRole, homeworkId, languageCode, messages);
        LOGGER.info("Homework " + homework.getId() + " content has been updated.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.HOMEWORK_CONTENT_UPDATED, botRole,
                new Localizations.Service.HomeworkContentUpdatedParams(homeworkId, homework.getMapping().getId())));
        LOGGER.debug("Message sent.");
    }

    public void toggleFeedbackInclusion(BotRole botRole, Long homeworkId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final Homework homework = homeworkService.toggleFeedbackInclusion(botRole, homeworkId);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.HOMEWORK_FEEDBACK_UPDATE_SUCCESS, botRole,
                new Localizations.Service.HomeworkFeedbackUpdateSuccessParams(getStatus(botRole, homework.isFeedbackRequired()))));
        LOGGER.debug("Message sent.");
    }

    public void toggleRepeatedCompletion(BotRole botRole, Long homeworkId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final Homework homework = homeworkService.toggleRepeatedCompletion(botRole, homeworkId);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.REPEATED_COMPLETION_UPDATE_SUCCESS, botRole,
                new Localizations.Service.RepeatedCompletionUpdateSuccessParams(getStatus(botRole, homework.isRepeatedCompletionAvailable()))));
        LOGGER.debug("Message sent.");
    }

    public void updateAllowedMediaTypes(BotRole botRole, Long homeworkId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);

        LOGGER.info("User " + botRole.getUser().getId() + " is trying to update allowed media types for homework " + homeworkId + ".");
        final String potentialMediaTypes = messages.getFirst().getText().trim().toUpperCase();

        try {
            final Homework homework = homeworkService.updateAllowedMediaTypes(botRole, homeworkId,
                    HomeworkMapper.parseMediaTypes(potentialMediaTypes));

            LOGGER.info("Media types updated to " + homework.getAllowedMediaTypes() + " for homework " + homework.getId() + ".");

            LOGGER.debug("Sending confirmation message...");
            clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.MEDIA_TYPES_UPDATE_SUCCESS, botRole,
                        new Localizations.Service.MediaTypesUpdateSuccessParams(homework.getAllowedMediaTypes())));
            LOGGER.debug("Message sent.");
        } catch (MediaTypeParseException e) {
            throw new InvalidDataSentException("Unable to parse provided media types: " + potentialMediaTypes + ".",
                    localizationLoader.localize(Localizations.Error.PARSE_MEDIA_TYPES_FAILURE, botRole,
                        new Localizations.Error.ParseMediaTypesFailureParams(Arrays.stream(MediaType.values())
                            .map(t -> t.toString())
                            .collect(Collectors.joining(", ")))));
        }
    }

    public void sendHomework(BotRole botRole, HomeworkProgress progress) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(progress, "Homework progress cannot be null");

        LOGGER.debug("Sending homework " + progress.getHomework().getId() + "'s content to user "
                + botRole.getUser().getId() + "...");
        final List<SendMessageResultDto> sentContent = contentService.sendLocalizedContent(
                botRole, progress.getHomework().getMapping().getId());

        LOGGER.debug("Content has been sent.");

        if (!checkAndHandleSendHomeworkStatusError(botRole, progress)) {
            final SendMessageResultDto menuMessage;

            if (sentContent.size() > 1) {
                LOGGER.debug("Content in homework " + progress.getHomework().getId() + " is a media group. "
                        + "It is a recomendation to avoid such cases since it requires an "
                        + "additional message to be sent for menu.");

                menuMessage = clientManager.sendMessage(botRole, localizationLoader
                        .localize(Localizations.Service.SEND_HOMEWORK_MEDIA_GROUP_BYPASS, botRole));
                LOGGER.debug("Additional message for menu has been sent.");
            } else {
                LOGGER.debug("Content in homework " + progress.getHomework().getId() + " is not a media group. "
                        + "Menu will be attached to it.");    
                menuMessage = sentContent.get(0);
            }
            if (menuMessage.getResult() == Result.OK) {
                menuService.initiateMenu(botRole, MenuKey.SEND_HOMEWORK, PROGRESS_ID_PARAM,
                        progress.getId().toString(), menuMessage.getMessage().getMessageId(),
                        MenuTerminationGroupKey.SEND_HOMEWORK, progress.getId());
                LOGGER.debug("Send homework has been sent to user " + botRole.getUser().getId() + " for homework "
                        + progress.getHomework().getId() + ".");
            } else {
                LOGGER.error("Failed to send the homework message for homework " + progress.getHomework().getId()
                        + " to user " + botRole.getUser().getId() + ".");
                // TODO: introduce fallback
            }
        }
    }

    public void commit(BotRole botRole, Long homeworkId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        HomeworkProgress progress = entityUtil.getHomeworkProgressByHomeworkId(botRole, homeworkId);
        final List<BotRole> mentors = userService.getHomeworkReceivingUsers(botRole.getBot().getId());

        if (progress.getHomework().getLesson().getCourse().isFeedbackIncluded()
                && progress.getHomework().isFeedbackRequired()
                && !mentors.isEmpty()) {
            progress = homeworkService.commit(botRole, homeworkId, messages, Status.AWAITS_APPROVAL);
            requestFeedback(botRole, progress, mentors);

            clientManager.sendMessage(botRole, localizationLoader.localize(
                    Localizations.Service.FEEDBACK_FOR_HOMEWORK_WAITING, botRole));
        } else {
            progress = homeworkService.commit(botRole, homeworkId, messages, Status.COMPLETED);

            for (final BotRole mentor : mentors) {
                clientManager.sendMessage(mentor, localizationLoader.localize(Localizations.Service.HOMEWORK_SUBMITTED_NOTIFICATION, mentor,
                    new Localizations.Service.HomeworkSubmittedNotificationParams(
                        progress.getUser().getId(),
                        progress.getUser().getFullName(),
                        localizationLoader.getLanguageName(mentor, progress.getUser().getLanguageCode())
                    )
                ));
                contentService.sendContentAsync(mentor, progress.getContent().getId());
            }
            clientManager.sendMessage(botRole, localizationLoader.localize(
                    Localizations.Service.HOMEWORK_ACCEPTED_AUTO, botRole));

            courseService.next(botRole, progress.getHomework().getLesson().getCourse().getId(),
                    progress.getHomework().getLesson().getId());
        }
        menuService.terminateMenuGroup(MenuTerminationGroupKey.SEND_HOMEWORK, progress.getId());
    }

    public void approve(BotRole botRole, Long progressId, List<Message> adminComment) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(progressId, "progressId cannot be null");
        Assert.notNull(adminComment, "adminComment cannot be null");

        HomeworkProgress progress = entityUtil.getHomeworkProgressById(botRole, progressId);

        if (!progress.getStatus().equals(Status.COMPLETED) && !progress.getStatus().equals(Status.DECLINED)) {

            progress = homeworkService.approve(botRole, progressId, adminComment);

            final BotRole targetRole = entityUtil.getActiveBotRole(botRole, progress.getUser().getId());
            final String courseName = contentService.getLocalizedText(targetRole,
                    progress.getHomework().getLesson().getCourse().getTitle().getId());

            if (!adminComment.isEmpty()) {
                clientManager.sendMessage(botRole, localizationLoader.localize(
                    Localizations.Service.HOMEWORK_APPROVED_NOTIFICATION_PLUS_COMMENT, botRole,
                    new Localizations.Service.HomeworkApprovedNotificationPlusCommentParams(
                        courseName,
                        progress.getHomework().getLesson().getPosition(),
                        botRole.getUser().getFullName(),
                        entityUtil.getLocalizedTitle(targetRole, botRole)
                    )
                ));
                contentService.sendContent(targetRole, progress.getLastComment());
            } else {
                clientManager.sendMessage(targetRole, localizationLoader.localize(
                    Localizations.Service.HOMEWORK_APPROVED_NOTIFICATION, targetRole,
                    new Localizations.Service.HomeworkApprovedNotificationParams(
                        courseName,
                        progress.getHomework().getLesson().getPosition(),
                        botRole.getUser().getFullName(),
                        entityUtil.getLocalizedTitle(targetRole, botRole)
                    )
                ));
            }

            courseService.next(targetRole, progress.getHomework().getLesson().getCourse().getId(),
                    progress.getHomework().getLesson().getId());
        }
        menuService.terminateMenuGroup(MenuTerminationGroupKey.REQUEST_FEEDBACK, progress.getId());
        removeFromFeedbackSession(botRole, progressId);
    }

    public void approve(BotRole botRole, Long progressId) {
        approve(botRole, progressId, List.of());
    }

    public void decline(BotRole botRole, Long progressId, List<Message> adminComment) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(progressId, "progressId cannot be null");
        Assert.notEmpty(adminComment, "adminComment cannot be empty or null");

        HomeworkProgress progress = entityUtil.getHomeworkProgressById(botRole, progressId);

        if (!progress.getStatus().equals(Status.COMPLETED) && !progress.getStatus().equals(Status.DECLINED)) {

            progress = homeworkService.decline(botRole, progressId, adminComment);

            final BotRole targetRole = entityUtil.getActiveBotRole(botRole, progress.getUser().getId());
            final String courseName = contentService.getLocalizedText(targetRole,
                    progress.getHomework().getLesson().getCourse().getTitle().getId());

            clientManager.sendMessage(targetRole, localizationLoader.localize(
                Localizations.Service.HOMEWORK_DECLINED_NOTIFICATION_PLUS_COMMENT, targetRole,
                new Localizations.Service.HomeworkDeclinedNotificationPlusCommentParams(
                    courseName,
                    progress.getHomework().getLesson().getPosition(),
                    botRole.getUser().getFullName(),
                    entityUtil.getLocalizedTitle(targetRole, botRole)
                )
            ));
            contentService.sendContent(targetRole, progress.getLastComment());
            sendHomework(targetRole, progress);
        }
        menuService.terminateMenuGroup(MenuTerminationGroupKey.REQUEST_FEEDBACK, progress.getId());
        removeFromFeedbackSession(botRole, progressId);
    }
    
    public void sendPendingHomeworks(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        final List<HomeworkProgress> progresses = homeworkService.getPendingHomeworksByBot(
                botRole.getBot().getId(), PageRequest.ofSize(pagedRequestProperties.homework().pageSize()));

        if (progresses.isEmpty()) {
            LOGGER.info("There are no pending homeworks in bot " + botRole.getBot().getId() + ".");
            clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.NO_PENDING_HOMEWORKS_IN_BOT, botRole));
            feedbackSessionRepository.remove(botRole.getId());
            return;
        }

        int counter = 0;
        for (final HomeworkProgress progress : progresses) {
            if (sendFeedbackMessage(botRole, progress.getHomework().getLesson().getCourse().getTitle(), progress)) ++counter;
        }

        feedbackSessionRepository.save(new HomeworkFeedbackSession(botRole.getId(), counter, null,
                progresses.stream().map(p -> p.getId()).toList()));
    }
    
    public void sendPendingHomeworks(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final List<HomeworkProgress> progresses = homeworkService.getPendingHomeworksByCourse(
                courseId, PageRequest.ofSize(pagedRequestProperties.homework().pageSize()));

        if (progresses.isEmpty()) {
            LOGGER.info("There are no pending homeworks for course " + courseId + ".");
            clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.NO_PENDING_HOMEWORKS_FOR_COURSE,
                    botRole, new Localizations.Service.NoPendingHomeworksForCourseParams(contentService.getLocalizedText(botRole,
                        entityUtil.getCourseTitle(botRole, courseId)))));
            feedbackSessionRepository.remove(botRole.getId());
            return;
        }

        int counter = 0;
        for (final HomeworkProgress progress : progresses) {
            if (sendFeedbackMessage(botRole, progress.getHomework().getLesson().getCourse().getTitle(), progress)) ++counter;
        }

        feedbackSessionRepository.save(new HomeworkFeedbackSession(botRole.getId(), counter, courseId,
                progresses.stream().map(p -> p.getId()).toList()));
    }

    private String getStatus(BotRole botRole, boolean status) {
        return status ? localizationLoader.localize(Localizations.Service.STATUS_ENABLED, botRole).getData()
                : localizationLoader.localize(Localizations.Service.STATUS_DISABLED, botRole).getData();
    }

    private void removeFromFeedbackSession(BotRole botRole, Long progressId) {
        final Optional<HomeworkFeedbackSession> feedbackSessionOpt = feedbackSessionRepository.find(botRole.getId());

        if (feedbackSessionOpt.isPresent()) {
            final var session = feedbackSessionOpt.get();

            if (session.getProgressIds().remove(progressId)) {
                session.setCounter(session.getCounter() - 1);
    
                if (session.getCounter() < 1) {
                    if (session.getCourseId() != null) {
                        sendPendingHomeworks(botRole, session.getCourseId());
                    } else {
                        sendPendingHomeworks(botRole);
                    }
                }
            }
        }
    }

    private void requestFeedback(BotRole botRole, HomeworkProgress progress, List<BotRole> mentors) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(progress, "Homework progress cannot be null");
        Assert.notEmpty(mentors, "mentors cannot be empty or null");

        final Course course = progress.getHomework().getLesson().getCourse();
        final ContentMapping courseTitle = entityUtil.getMappingById(botRole, course.getTitle().getId());

        for (final BotRole mentor : mentors) {
            LOGGER.debug("User " + mentor.getId() + " has homework feedback enabled. "
                    + "Sending approval message to them...");

            sendFeedbackMessage(mentor, courseTitle, progress);
        }
    }

    private boolean checkAndHandleSendHomeworkStatusError(BotRole botRole, HomeworkProgress homeworkProgress) {
        final Homework homework = homeworkProgress.getHomework();

        switch (homeworkProgress.getStatus()) {
            case AWAITS_APPROVAL:
                LOGGER.debug("User " + botRole.getUser().getId() + " is currently awaiting feedback for "
                        + "homework " + homework.getId() + ".");
                clientManager.sendMessage(botRole, localizationLoader.localize(
                        Error.HOMEWORK_ALREADY_AWAITS_APPROVAL, botRole));
                
                return true;
            case COMPLETED:
                if (homework.isRepeatedCompletionAvailable()) {
                    LOGGER.debug("User " + botRole.getUser().getId() + " has already completed homework "
                            + homework.getId() + " but since it supports repeated homework "
                            + "completions, they will be able to send it again. This can be "
                            + "disabled in course settings.");
                    return false;
                }
                LOGGER.debug("User " + botRole.getUser().getId() + " has already completed homework "
                        + homework.getId());

                final Course course = homework.getLesson().getCourse();
                final CourseProgress courseProgress = entityUtil.getCourseProgressForUser(botRole, course.getId());

                if (courseProgress.getStage() >= course.getLessons().size() - 1) {
                    LOGGER.info("User " + botRole.getUser().getId() + " has completed course "
                            + course.getId() + ". Commencing ending sequence...");
                    courseService.next(botRole, course.getId(), homework.getLesson().getId());

                    return true;
                }
                LOGGER.debug( "Triggering next stage menu...");

                final SendMessageResultDto message = clientManager.sendMessage(botRole,
                        localizationLoader.localize(Error.HOMEWORK_ALREADY_COMPLETED, botRole));

                if (message.getResult() == Result.OK) {
                    menuService.initiateMenu(botRole, MenuKey.COURSE_NEXT_STAGE, LESSON_ID_PARAM,
                            homework.getLesson().getId().toString(), message.getMessage().getMessageId(),
                            MenuTerminationGroupKey.COURSE_NEXT_STAGE, courseProgress.getId());
                } else {
                    LOGGER.error("Failed to send next stage menu for user " + botRole.getUser().getId() + ".");
                    // TODO: introduce fallback
                }
                return true;
            default:
                return false;
        }
    }

    private boolean sendFeedbackMessage(BotRole mentorBotRole, ContentMapping courseTitle, HomeworkProgress progress) {
        clientManager.sendMessage(mentorBotRole, localizationLoader.localize(Localizations.Service.HOMEWORK_FEEDBACK_REQUEST_NOTIFICATION, mentorBotRole,
            new Localizations.Service.HomeworkFeedbackRequestNotificationParams(
                progress.getUser().getId(),
                progress.getUser().getFullName(),
                localizationLoader.getLanguageName(mentorBotRole, progress.getUser().getLanguageCode()),
                contentService.getLocalizedText(mentorBotRole, courseTitle),
                progress.getHomework().getLesson().getPosition()
            )
        ));

        LOGGER.debug("Homework feedback info has been sent to user " + mentorBotRole.getUser().getId() + ".");
        final List<SendMessageResultDto> sentContent = contentService.sendContent(mentorBotRole, progress.getContent());

        LOGGER.debug("Homework content has been sent to user " + mentorBotRole.getUser().getId() + ". Attaching menu...");
        final SendMessageResultDto menuMessage;

        if (sentContent.size() > 1) {
            LOGGER.debug("Homework progress " + progress.getId()
                    + "'s content is a media group. To avoid Telegram restrictions, an "
                    + "additional message will be sent to user " + mentorBotRole.getUser().getId()
                    + " to attach the feedback menu to.");
            menuMessage = clientManager.sendMessage(mentorBotRole, localizationLoader
                    .localize(Localizations.Service.FEEDBACK_MEDIA_GROUP_BYPASS, mentorBotRole));
            LOGGER.debug("Additional message for menu has been sent.");
        } else {
            LOGGER.debug("Homework progress " + progress.getId() + "'s content "
                    + "is not a media group. Menu will be attached to it.");    
            menuMessage = sentContent.get(0);
        }
        if (menuMessage.getResult() == Result.OK) {
            menuService.initiateMenu(mentorBotRole, MenuKey.REQUEST_FEEDBACK, PROGRESS_ID_PARAM,
                    progress.getId().toString(), menuMessage.getMessage().getMessageId(),
                    MenuTerminationGroupKey.REQUEST_FEEDBACK, progress.getId());
            LOGGER.debug("Feedback menu has been initialized for user " + mentorBotRole.getUser().getId() + ".");
            return true;
        } else {
            LOGGER.error("Failed to send a homework feedback message to user " + mentorBotRole.getUser().getId() + ".");
            // TODO: introduce fallback
            return false;
        }
    }
}
