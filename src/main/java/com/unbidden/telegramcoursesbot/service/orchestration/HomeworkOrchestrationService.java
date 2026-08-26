package com.unbidden.telegramcoursesbot.service.orchestration;

import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.HomeworkResponseDto;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.MediaTypeParseException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.mapper.HomeworkMapper;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.MenuTerminationGroupKey;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress.Status;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

@Service
public class HomeworkOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(HomeworkOrchestrationService.class);

    private static final String PROGRESS_ID_PARAM = "progressId";
    private static final String LESSON_ID_PARAM = "lessonId";

    public static final int MAX_HOMEWORK_DELAY = 720;

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

    public HomeworkOrchestrationService(HomeworkService homeworkService, TimingService timingService,
            ContentOrchestrationService contentService, MenuOrchestrationService menuService, UserService userService,
            HomeworkMapper mapper, LocalizationLoader localizationLoader, ClientManager clientManager,
            EntityUtil entityUtil, @Lazy CourseOrchestrationService courseService, ValidatorUtil validatorUtil) {
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
    }

    public HomeworkResponseDto getById(UserEntity user, Bot bot, Long homeworkId) {
        return mapper.toDto(entityUtil.getHomeworkById(user, bot, homeworkId));
    }

    public void initHomework(UserEntity user, Bot bot, Long homeworkId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final HomeworkProgress progress = homeworkService.createOrLoadProgress(user, bot, homeworkId);

        if (timingService.existsHomeworkTrigger(user.getId(), homeworkId)) {
            LOGGER.debug("User " + user.getId() + " is currently awaiting homework.");
            return;
        }
        
        sendHomework(progress);
    }

    public void createHomework(UserEntity user, Bot bot, Long lessonId, List<Message> messages) {
        LOGGER.info("User " + user.getId() + " want to add a new homework to lesson " + lessonId + ".");

        validatorUtil.checkAtLeastExpectedMessages(user, messages, 1);

        String languageCode = user.getLanguageCode();
        if (messages.size() > 1 && validatorUtil.checkLanguageCode(user, messages.getLast())) {
            languageCode = messages.getLast().getText();
            messages.removeLast();
        }

        final Homework homework = homeworkService.createHomework(user, bot, lessonId, languageCode, messages);

        LOGGER.info("New Homework " + homework.getId() + " has been created for lesson " + lessonId + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(Localizations.Service.NEW_HOMEWORK_CREATED,
                user, new Localizations.Service.NewHomeworkCreatedParams(homework.getId())));
        LOGGER.debug("Message sent.");
    }

    public void updateDelay(UserEntity user, Bot bot, Long homeworkId, List<Message> messages) {
        validatorUtil.checkExactExpectedMessages(user, messages, 1);
        LOGGER.info("User " + user.getId() + " is trying to update delay for homework " + homeworkId + "...");
            
        final int newDelay = Math.clamp(validatorUtil.parseIntInBounds(user, messages.getFirst(),
                Integer.MIN_VALUE, MAX_HOMEWORK_DELAY), 0, MAX_HOMEWORK_DELAY);
        final Homework homework = homeworkService.updateDelay(user, bot, homeworkId, newDelay);

        LOGGER.info("Homework " + homework.getId() + " now has a delay of " + homework.getDelay() + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .localize(Localizations.Service.NEW_DELAY_SET_SUCCESS, user));
        LOGGER.debug("Message sent.");
    }

    public void updateContent(UserEntity user, Bot bot, Long homeworkId, List<Message> messages) {
        validatorUtil.checkAtLeastExpectedMessages(user, messages, 2);
        LOGGER.info("User " + user.getId() + " is trying to update homework " + homeworkId + "...");  

        final String languageCode = validatorUtil.checkLanguageCode(user, messages.getLast())
                ? messages.getLast().getText().trim()
                : user.getLanguageCode();

        final Homework homework = homeworkService.updateContent(user, bot, homeworkId, languageCode, messages);
        LOGGER.info("Homework " + homework.getId() + " content has been updated.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.HOMEWORK_CONTENT_UPDATED, user,
                new Localizations.Service.HomeworkContentUpdatedParams(homeworkId, homework.getMapping().getId())));
        LOGGER.debug("Message sent.");
    }

    public void toggleFeedbackInclusion(UserEntity user, Bot bot, Long homeworkId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final Homework homework = homeworkService.toggleFeedbackInclusion(user, bot, homeworkId);

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.HOMEWORK_FEEDBACK_UPDATE_SUCCESS, user,
                new Localizations.Service.HomeworkFeedbackUpdateSuccessParams(getStatus(user, homework.isFeedbackRequired()))));
        LOGGER.debug("Message sent.");
    }

    public void toggleRepeatedCompletion(UserEntity user, Bot bot, Long homeworkId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final Homework homework = homeworkService.toggleRepeatedCompletion(user, bot, homeworkId);

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.REPEATED_COMPLETION_UPDATE_SUCCESS, user,
                new Localizations.Service.RepeatedCompletionUpdateSuccessParams(getStatus(user, homework.isRepeatedCompletionAvailable()))));
        LOGGER.debug("Message sent.");
    }

    public void updateAllowedMediaTypes(UserEntity user, Bot bot, Long homeworkId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        validatorUtil.checkExactExpectedMessages(user, messages, 1);

        LOGGER.info("User " + user.getId() + " is trying to update allowed media types for homework " + homeworkId + ".");
        final String potentialMediaTypes = messages.getFirst().getText().trim().toUpperCase();

        try {
            final Homework homework = homeworkService.updateAllowedMediaTypes(user, bot, homeworkId,
                    HomeworkMapper.parseMediaTypes(potentialMediaTypes));

            LOGGER.info("Media types updated to " + homework.getAllowedMediaTypes() + " for homework " + homework.getId() + ".");

            LOGGER.debug("Sending confirmation message...");
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .localize(Localizations.Service.MEDIA_TYPES_UPDATE_SUCCESS, user,
                        new Localizations.Service.MediaTypesUpdateSuccessParams(homework.getAllowedMediaTypes())));
            LOGGER.debug("Message sent.");
        } catch (MediaTypeParseException e) {
            throw new InvalidDataSentException("Unable to parse provided media types: " + potentialMediaTypes + ".",
                    localizationLoader.localize(Localizations.Error.PARSE_MEDIA_TYPES_FAILURE, user,
                        new Localizations.Error.ParseMediaTypesFailureParams(Arrays.stream(MediaType.values())
                            .map(t -> t.toString())
                            .collect(Collectors.joining(" ")))));
        }
    }

    public void sendHomework(HomeworkProgress progress) {
        Assert.notNull(progress, "Homework progress cannot be null");

        final UserEntity user = progress.getUser();
        final Homework homework = progress.getHomework();
        final Bot bot = homework.getLesson().getCourse().getBot();

        LOGGER.debug("Sending homework " + homework.getId() + "'s content to user "
                + user.getId() + "...");
        final List<Message> sentContent = contentService.sendLocalizedContent(user, bot, homework.getMapping().getId());

        LOGGER.debug("Content has been sent.");

        if (!checkAndHandleSendHomeworkStatusError(progress)) {
            sendHomeworkMenu(sentContent, homework, user, progress);
        }
    }

    public void commit(UserEntity user, Bot bot, Long homeworkId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        HomeworkProgress progress = entityUtil.getHomeworkProgressByHomeworkId(user, bot, homeworkId);
        final List<UserEntity> mentors = userService.getHomeworkReceivingUsers(bot);

        if (progress.getHomework().getLesson().getCourse().isFeedbackIncluded()
                && progress.getHomework().isFeedbackRequired()
                && !mentors.isEmpty()) {
            progress = homeworkService.commit(user, bot, homeworkId, messages, Status.AWAITS_APPROVAL);
            requestFeedback(progress, mentors);

            clientManager.getClient(bot).sendMessage(progress.getUser(),
                    localizationLoader.localize(
                    Localizations.Service.FEEDBACK_FOR_HOMEWORK_WAITING, progress.getUser()));
        } else {
            progress = homeworkService.commit(user, bot, homeworkId, messages, Status.COMPLETED);

            for (final UserEntity mentor : mentors) {
                clientManager.getClient(bot).sendMessage(mentor,
                    localizationLoader.localize(Localizations.Service.HOMEWORK_SUBMITTED_NOTIFICATION, mentor,
                        new Localizations.Service.HomeworkSubmittedNotificationParams(
                            progress.getUser().getId(),
                            progress.getUser().getFullName(),
                            localizationLoader.getLanguageName(mentor, progress.getUser().getLanguageCode())
                        )
                    )
                );
                contentService.sendContent(mentor, bot, progress.getContent().getId());
            }
            clientManager.getClient(bot).sendMessage(progress.getUser(),
                    localizationLoader.localize(
                    Localizations.Service.HOMEWORK_ACCEPTED_AUTO, progress.getUser()));

            courseService.next(user, bot, progress.getHomework().getLesson().getCourse().getId(),
                    progress.getHomework().getLesson().getId());
        }
        menuService.terminateMenuGroup(MenuTerminationGroupKey.SEND_HOMEWORK, progress.getId());
    }

    public void approve(UserEntity mentor, Bot bot, Long progressId, List<Message> adminComment) {
        Assert.notNull(mentor, "mentor cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(progressId, "progressId cannot be null");
        Assert.notNull(adminComment, "adminComment cannot be null");

        HomeworkProgress progress = entityUtil.getHomeworkProgressById(mentor, bot, progressId);

        if (!progress.getStatus().equals(Status.COMPLETED) && !progress.getStatus().equals(Status.DECLINED)) {

            progress = homeworkService.approve(mentor, bot, progressId, adminComment);

            menuService.terminateMenuGroup(MenuTerminationGroupKey.REQUEST_FEEDBACK, progress.getId());
            final String courseName = contentService.getLocalizedText(progress.getUser(), bot,
                    progress.getHomework().getLesson().getCourse().getTitle().getId());

            if (!adminComment.isEmpty()) {
                clientManager.getClient(bot).sendMessage(progress.getUser(), localizationLoader.localize(
                    Localizations.Service.HOMEWORK_APPROVED_NOTIFICATION_PLUS_COMMENT, progress.getUser(),
                    new Localizations.Service.HomeworkApprovedNotificationPlusCommentParams(courseName,
                        progress.getHomework().getLesson().getPosition(), mentor.getFullName(),
                        entityUtil.getLocalizedTitle(progress.getUser(), bot, mentor))));
                contentService.sendContent(progress.getUser(), bot, progress.getLastComment().getId());
            } else {
                clientManager.getClient(bot).sendMessage(progress.getUser(), localizationLoader.localize(
                    Localizations.Service.HOMEWORK_APPROVED_NOTIFICATION, progress.getUser(),
                    new Localizations.Service.HomeworkApprovedNotificationParams(courseName,
                        progress.getHomework().getLesson().getPosition(), mentor.getFullName(),
                        entityUtil.getLocalizedTitle(progress.getUser(), bot, mentor))));
            }

            courseService.next(progress.getUser(), bot, progress.getHomework().getLesson().getCourse().getId(),
                    progress.getHomework().getLesson().getId());
        }
    }

    public void approve(UserEntity mentor, Bot bot, Long progressId) {
        approve(mentor, bot, progressId, List.of());
    }

    public void decline(UserEntity mentor, Bot bot, Long progressId, List<Message> adminComment) {
        Assert.notNull(mentor, "mentor cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(progressId, "progressId cannot be null");
        Assert.notEmpty(adminComment, "adminComment cannot be empty or null");

        HomeworkProgress progress = entityUtil.getHomeworkProgressById(mentor, bot, progressId);

        if (!progress.getStatus().equals(Status.COMPLETED) && !progress.getStatus().equals(Status.DECLINED)) {

            progress = homeworkService.decline(mentor, bot, progressId, adminComment);

            menuService.terminateMenuGroup(MenuTerminationGroupKey.REQUEST_FEEDBACK, progress.getId());
            final String courseName = contentService.getLocalizedText(progress.getUser(), bot, progress.getHomework().getLesson()
                    .getCourse().getTitle().getId());

            clientManager.getClient(bot).sendMessage(progress.getUser(), localizationLoader.localize(
                    Localizations.Service.HOMEWORK_DECLINED_NOTIFICATION_PLUS_COMMENT, progress.getUser(),
                    new Localizations.Service.HomeworkDeclinedNotificationPlusCommentParams(courseName,
                        progress.getHomework().getLesson().getPosition(), mentor.getFullName(),
                        entityUtil.getLocalizedTitle(progress.getUser(), bot, mentor))));
            contentService.sendContent(progress.getUser(), bot, progress.getLastComment().getId());
            sendHomework(progress);
        }
    }
    
    private String getStatus(UserEntity user, boolean status) {
        return status ? localizationLoader.localize(Localizations.Service.STATUS_ENABLED, user).getData()
                : localizationLoader.localize(Localizations.Service.STATUS_DISABLED, user).getData();
    }

    private void requestFeedback(HomeworkProgress progress, List<UserEntity> mentors) {
        Assert.notNull(progress, "Homework progress cannot be null");

        final Bot bot = progress.getHomework().getLesson().getCourse().getBot();
        
        final UserEntity target = progress.getUser();
        final Course course = progress.getHomework().getLesson().getCourse();
        
        for (final UserEntity mentor : mentors) {
            LOGGER.debug("User " + mentor.getId() + " has homework feedback enabled. "
                    + "Sending approval message to them...");

            clientManager.getClient(bot).sendMessage(mentor,
                    localizationLoader.localize(Localizations.Service.HOMEWORK_FEEDBACK_REQUEST_NOTIFICATION, mentor,
                        new Localizations.Service.HomeworkFeedbackRequestNotificationParams(
                            target.getId(),
                            target.getFullName(),
                            localizationLoader.getLanguageName(mentor, target.getLanguageCode()),
                            contentService.getLocalizedText(mentor, bot, course.getTitle().getId()),
                            progress.getHomework().getLesson().getPosition()
                        )
                    )
            );
            LOGGER.debug("Homework feedback info has been sent to user " + mentor.getId() + ".");
            final List<Message> sentContent = contentService.sendContent(mentor, bot, progress
                    .getContent().getId());

            LOGGER.debug("Homework content has been sent to user " + mentor.getId() + ".");

            final Message menuMessage;
            if (sentContent.size() > 1) {
                LOGGER.debug("Homework progress " + progress.getId()
                        + "'s content is a media group. To avoid Telegram restrictions, an "
                        + "additional message will be sent to user " + mentor.getId()
                        + " to attach the feedback menu to.");
                menuMessage = clientManager.getClient(bot).sendMessage(mentor, localizationLoader
                        .localize(Localizations.Service.FEEDBACK_MEDIA_GROUP_BYPASS, mentor));
                LOGGER.debug("Additional message for menu has been sent.");
            } else {
                LOGGER.debug("Homework progress " + progress.getId() + "'s content "
                        + "is not a media group. Menu will be attached to it.");    
                menuMessage = sentContent.get(0);
            }
            menuService.initiateMenu(mentor, bot, MenuKey.REQUEST_FEEDBACK, PROGRESS_ID_PARAM,
                    progress.getId().toString(), menuMessage.getMessageId(), MenuTerminationGroupKey.REQUEST_FEEDBACK, progress.getId());
            LOGGER.debug("Feedback menu has been initialized for user " + mentor.getId() + ".");
        }
    }

    private boolean checkAndHandleSendHomeworkStatusError(HomeworkProgress homeworkProgress) {
        final UserEntity user = homeworkProgress.getUser();
        final Homework homework = homeworkProgress.getHomework();
        final Bot bot = homework.getLesson().getCourse().getBot();

        switch (homeworkProgress.getStatus()) {
            case AWAITS_APPROVAL:
                LOGGER.debug("User " + user.getId() + " is currently awaiting feedback for "
                        + "homework " + homework.getId() + ".");
                clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                        Error.HOMEWORK_ALREADY_AWAITS_APPROVAL, user));
                
                return true;
            case COMPLETED:
                if (homework.isRepeatedCompletionAvailable()) {
                    LOGGER.debug("User " + user.getId() + " has already completed homework "
                            + homework.getId() + " but since it supports repeated homework "
                            + "completions, they will be able to send it again. This can be "
                            + "disabled in course settings.");
                    return false;
                }
                LOGGER.debug("User " + user.getId() + " has already completed homework "
                        + homework.getId());

                final Course course = homework.getLesson().getCourse();
                final CourseProgress courseProgress = entityUtil.getCourseProgressForUser(user, bot, course.getId());

                if (courseProgress.getStage() >= course.getLessons().size() - 1) {
                    LOGGER.info("User " + user.getId() + " has completed course "
                            + course.getId() + ". Commencing ending sequence...");
                    courseService.next(user, bot, course.getId(), homework.getLesson().getId());

                    return true;
                }
                LOGGER.debug( "Triggering next stage menu...");

                final Message message = clientManager.getClient(bot).sendMessage(user,
                        localizationLoader.localize(Error.HOMEWORK_ALREADY_COMPLETED, user));

                menuService.initiateMenu(user, bot, MenuKey.COURSE_NEXT_STAGE, LESSON_ID_PARAM,
                        homework.getLesson().getId().toString(), message.getMessageId(),
                        MenuTerminationGroupKey.COURSE_NEXT_STAGE, courseProgress.getId());
                
                return true;
            default:
                return false;
        }
    }

    private void sendHomeworkMenu(List<Message> sentContent, Homework homework,
            UserEntity user, HomeworkProgress progress) {
        final Bot bot = homework.getLesson().getCourse().getBot();

        final Message menuMessage;
        if (sentContent.size() > 1) {
            LOGGER.debug("Content in homework " + homework.getId() + " is a media group. "
                    + "It is a recomendation to avoid such cases since it requires an "
                    + "additional message to be sent for menu.");
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .localize(Localizations.Service.SEND_HOMEWORK_MEDIA_GROUP_BYPASS, user);
            menuMessage = clientManager.getClient(bot).sendMessage(user, mediaGroupBypassMessageLoc);
            LOGGER.debug("Additional message for menu has been sent.");
        } else {
            LOGGER.debug("Content in homework " + homework.getId() + " is not a media group. "
                    + "Menu will be attached to it.");    
            menuMessage = sentContent.get(0);
        }
        menuService.initiateMenu(user, bot, MenuKey.SEND_HOMEWORK, PROGRESS_ID_PARAM,
                progress.getId().toString(), menuMessage.getMessageId(), MenuTerminationGroupKey.SEND_HOMEWORK, progress.getId());
    }
}
