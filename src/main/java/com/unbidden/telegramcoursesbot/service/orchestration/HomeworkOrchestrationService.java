package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress.Status;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

@Service
public class HomeworkOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(HomeworkOrchestrationService.class);

    private static final String SEND_HOMEWORK_MENU = "m_sHw";
    private static final String COURSE_NEXT_STAGE_MENU = "m_crsNxtStg";
    private static final String REQUEST_FEEDBACK_MENU = "m_rqF";

    private static final String FEEDBACK_MENU_TERMINATION = "homework_progress_%s_feedback_menus";
    private static final String SEND_HOMEWORK_MENU_TERMINATION = "homework_progress_%s_send_homework_menus";

    private final HomeworkService homeworkService;

    private final TimingService timingService;

    private final ContentService contentService;

    private final MenuService menuService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    private final CourseOrchestrationService courseService;

    public HomeworkOrchestrationService(HomeworkService homeworkService, TimingService timingService,
            ContentService contentService, MenuService menuService, UserService userService,
            LocalizationLoader localizationLoader, ClientManager clientManager, EntityUtil entityUtil,
            @Lazy CourseOrchestrationService courseService) {
        this.homeworkService = homeworkService;
        this.timingService = timingService;
        this.contentService = contentService;
        this.menuService = menuService;
        this.userService = userService;
        this.localizationLoader = localizationLoader;
        this.clientManager = clientManager;
        this.entityUtil = entityUtil;
        this.courseService = courseService;
    }

    public void initHomework(UserEntity user, Bot bot, Long homeworkId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final HomeworkProgress progress = homeworkService.createOrLoadProgress(user, homeworkId);

        if (timingService.existsHomeworkTrigger(user.getId(), homeworkId)) {
            LOGGER.debug("User " + user.getId() + " is currently awaiting homework.");
            return;
        }
        
        sendHomework(progress);
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

        if (progress.getHomework().getLesson().getCourse().isFeedbackIncluded()
                && progress.getHomework().isFeedbackRequired()
                && requestFeedback(progress)) {
            progress = homeworkService.commit(user, bot, homeworkId, messages, Status.AWAITS_APPROVAL);

            clientManager.getClient(bot).sendMessage(progress.getUser(),
                    localizationLoader.getLocalizationForUser(
                    Localizations.Service.FEEDBACK_FOR_HOMEWORK_WAITING, progress.getUser()));
        } else {
            progress = homeworkService.commit(user, bot, homeworkId, messages, Status.COMPLETED);

            final List<UserEntity> mentors = userService.getHomeworkReceivingUsers(bot);

            for (final UserEntity mentor : mentors) {
                clientManager.getClient(bot).sendMessage(mentor,
                        localizationLoader.getLocalizationForUser(
                        Localizations.Service.HOMEWORK_SUBMITTED_NOTIFICATION, mentor,
                        new Localizations.Service.HomeworkSubmittedNotificationParams(progress.getUser().getId(),
                                progress.getUser().getFullName(), progress.getUser().getLanguageCode())));
                contentService.sendContent(mentor, bot, progress.getContent().getId());
            }
            clientManager.getClient(bot).sendMessage(progress.getUser(),
                    localizationLoader.getLocalizationForUser(
                    Localizations.Service.HOMEWORK_ACCEPTED_AUTO, progress.getUser()));

            courseService.next(user, bot, progress.getHomework().getLesson().getCourse().getId());
        }
        menuService.terminateMenuGroup(progress.getUser(), bot,
                SEND_HOMEWORK_MENU_TERMINATION.formatted(progress.getId()));
    }

    public void approve(UserEntity mentor, Bot bot, UserEntity target,
            Long homeworkId, List<Message> adminComment) {
        Assert.notNull(mentor, "mentor cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(target, "target cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(adminComment, "adminComment cannot be null");

        HomeworkProgress progress = entityUtil.getHomeworkProgressByHomeworkId(target, bot, homeworkId);

        if (!progress.getStatus().equals(Status.COMPLETED) &&
                !progress.getStatus().equals(Status.DECLINED)) {

            progress = homeworkService.approve(mentor, bot, target, homeworkId, adminComment);

            menuService.terminateMenuGroup(progress.getUser(), bot,
                    FEEDBACK_MENU_TERMINATION.formatted(progress.getId()));
            final String courseName = contentService.getLocalizedText(target, bot, progress.getHomework().getLesson().getCourse().getTitle().getId());

            if (!adminComment.isEmpty()) {
                clientManager.getClient(bot).sendMessage(target, localizationLoader.getLocalizationForUser(
                    Localizations.Service.HOMEWORK_APPROVED_NOTIFICATION_PLUS_COMMENT, target,
                    new Localizations.Service.HomeworkApprovedNotificationPlusCommentParams(courseName,
                        progress.getHomework().getLesson().getPosition(), mentor.getFullName(),
                        entityUtil.getLocalizedTitle(target, bot, mentor))));
                contentService.sendContent(target, bot, progress.getLastComment().getId());
            } else {
                clientManager.getClient(bot).sendMessage(target, localizationLoader.getLocalizationForUser(
                    Localizations.Service.HOMEWORK_APPROVED_NOTIFICATION, target,
                    new Localizations.Service.HomeworkApprovedNotificationParams(courseName,
                        progress.getHomework().getLesson().getPosition(), mentor.getFullName(),
                        entityUtil.getLocalizedTitle(target, bot, mentor))));
            }

            courseService.next(target, bot, progress.getHomework().getLesson().getCourse().getId());
        }
    }

    public void approve(UserEntity mentor, Bot bot, UserEntity target,
            Long homeworkId) {
        approve(mentor, bot, target, homeworkId, List.of());
    }

    public void decline(UserEntity mentor, Bot bot, UserEntity target,
            Long homeworkId, List<Message> adminComment) {
        Assert.notNull(mentor, "mentor cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(target, "target cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(adminComment, "adminComment cannot be null");

        HomeworkProgress progress = entityUtil.getHomeworkProgressByHomeworkId(target, bot, homeworkId);

        if (!progress.getStatus().equals(Status.COMPLETED) &&
                !progress.getStatus().equals(Status.DECLINED)) {

            progress = homeworkService.decline(mentor, bot, target, homeworkId, adminComment);

            menuService.terminateMenuGroup(progress.getUser(), bot,
                    FEEDBACK_MENU_TERMINATION.formatted(progress.getId()));
            final String courseName = contentService.getLocalizedText(target, bot, progress.getHomework().getLesson().getCourse().getTitle().getId());

            clientManager.getClient(bot).sendMessage(target, localizationLoader.getLocalizationForUser(
                    Localizations.Service.HOMEWORK_DECLINED_NOTIFICATION_PLUS_COMMENT, target,
                    new Localizations.Service.HomeworkDeclinedNotificationPlusCommentParams(courseName,
                        progress.getHomework().getLesson().getPosition(), mentor.getFullName(),
                        entityUtil.getLocalizedTitle(target, bot, mentor))));
            contentService.sendContent(target, bot, progress.getLastComment().getId());
        }
    }

    private boolean requestFeedback(HomeworkProgress progress) {
        Assert.notNull(progress, "Homework progress cannot be null");

        final Bot bot = progress.getHomework().getLesson().getCourse().getBot();
        final List<UserEntity> mentors = userService.getHomeworkReceivingUsers(bot);
        final UserEntity target = progress.getUser();
        final Course course = progress.getHomework().getLesson().getCourse();
        
        LOGGER.debug("Checking if there are any users who are receiving "
                + "homework feedback requests...");
        if (mentors.isEmpty()) {
            LOGGER.info("There are no users who are receiving homework feedback in bot "
                    + bot.getId() + ". This means homework inclusion in course settings "
                    + "will be ignored.");
            return false;
        }
        for (final UserEntity mentor : mentors) {
            LOGGER.debug("User " + mentor.getId() + " has homework feedback enabled. "
                    + "Sending approval message to them...");

            clientManager.getClient(bot).sendMessage(mentor,
                    localizationLoader.getLocalizationForUser(Localizations.Service.HOMEWORK_FEEDBACK_REQUEST_NOTIFICATION,
                    mentor, new Localizations.Service.HomeworkFeedbackRequestNotificationParams(target.getId(),
                    target.getFullName(), target.getLanguageCode(), contentService.getLocalizedText(mentor, bot,
                        course.getTitle().getId()), progress.getHomework().getLesson().getPosition())));
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
                        .getLocalizationForUser(Localizations.Service.FEEDBACK_MEDIA_GROUP_BYPASS, mentor));
                LOGGER.debug("Additional message for menu has been sent.");
            } else {
                LOGGER.debug("Homework progress " + progress.getId() + "'s content "
                        + "is not a media group. Menu will be attached to it.");    
                menuMessage = sentContent.get(0);
            }
            menuService.initiateMenu(mentor, bot, REQUEST_FEEDBACK_MENU, progress.getId().toString(),
                    menuMessage.getMessageId());
            menuService.addToMenuTerminationGroup(progress.getUser(), mentor, bot,
                    menuMessage.getMessageId(), FEEDBACK_MENU_TERMINATION.formatted(
                    progress.getId()), null);
            LOGGER.debug("Feedback menu has been initialized for user " + mentor.getId() + ".");
        }
        return true;
    }

    private boolean checkAndHandleSendHomeworkStatusError(HomeworkProgress homeworkProgress) {
        final UserEntity user = homeworkProgress.getUser();
        final Homework homework = homeworkProgress.getHomework();
        final Bot bot = homework.getLesson().getCourse().getBot();

        switch (homeworkProgress.getStatus()) {
            case AWAITS_APPROVAL:
                LOGGER.debug("User " + user.getId() + " is currently awaiting feedback for "
                        + "homework " + homework.getId() + ".");
                clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
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

                if (courseProgress.getStage().equals(course.getNumberOfLessons() - 1)) {
                    LOGGER.info("User " + user.getId() + " has completed course "
                            + course.getId() + ". Commencing ending sequence...");
                    courseService.next(user, bot, course.getId());

                    return true;
                }
                LOGGER.debug( "Triggering next stage menu...");

                final Message message = clientManager.getClient(bot).sendMessage(user,
                        localizationLoader.getLocalizationForUser(Error.HOMEWORK_ALREADY_COMPLETED, user));

                menuService.initiateMenu(user, bot, COURSE_NEXT_STAGE_MENU, course.getId()
                        + CourseOrchestrationService.COURSE_NAME_LESSON_INDEX_DIVIDER + courseProgress.getStage(),
                        message.getMessageId());
                menuService.addToMenuTerminationGroup(user, user, bot, message.getMessageId(),
                        CourseOrchestrationService.COURSE_NEXT_STAGE_MENU_TERMINATION
                        .formatted(courseProgress.getId()), null);
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
                    .getLocalizationForUser(Localizations.Service.SEND_HOMEWORK_MEDIA_GROUP_BYPASS, user);
            menuMessage = clientManager.getClient(bot).sendMessage(user, mediaGroupBypassMessageLoc);
            LOGGER.debug("Additional message for menu has been sent.");
        } else {
            LOGGER.debug("Content in homework " + homework.getId() + " is not a media group. "
                    + "Menu will be attached to it.");    
            menuMessage = sentContent.get(0);
        }
        menuService.initiateMenu(user, bot, SEND_HOMEWORK_MENU, progress.getId().toString(),
                menuMessage.getMessageId());
        menuService.addToMenuTerminationGroup(user, user, bot, menuMessage.getMessageId(),
                SEND_HOMEWORK_MENU_TERMINATION.formatted(progress.getId()), null);
    }
}
