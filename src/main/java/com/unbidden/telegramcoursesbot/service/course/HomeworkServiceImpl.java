package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.AccessDeniedException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress.Status;
import com.unbidden.telegramcoursesbot.model.HomeworkTrigger;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.TimedTrigger;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkProgressRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class HomeworkServiceImpl implements HomeworkService {
    private static final Logger LOGGER = LogManager.getLogger(HomeworkServiceImpl.class);
    
    private static final String REQUEST_FEEDBACK_MENU = "m_rqF";
    private static final String SEND_HOMEWORK_MENU = "m_sHw";
    private static final String COURSE_NEXT_STAGE_MENU = "m_crsNxtStg";

    private static final String PARAM_TARGET_LANGUAGE_CODE = "${targetLanguageCode}";
    private static final String PARAM_TARGET_USERNAME = "${targetUsername}";
    private static final String PARAM_TARGET_LAST_NAME = "${targetLastName}";
    private static final String PARAM_TARGET_FIRST_NAME = "${targetFirstName}";
    private static final String PARAM_TARGET_ID = "${targetId}";
    private static final String PARAM_COMMENTER_NAME = "${commenterName}";
    private static final String PARAM_LESSON_INDEX = "${lessonIndex}";
    private static final String PARAM_COURSE_NAME = "${courseName}";
    private static final String PARAM_TITLE = "${title}";

    private static final String SERVICE_HOMEWORK_DECLINED_NOTIFICATION_PLUS_COMMENT =
            "service_homework_declined_notification_plus_comment";
    private static final String SERVICE_HOMEWORK_APPROVED_NOTIFICATION =
            "service_homework_approved_notification";
    private static final String SERVICE_HOMEWORK_APPROVED_NOTIFICATION_PLUS_COMMENT =
            "service_homework_approved_notification_plus_comment";
    private static final String SERVICE_HOMEWORK_FEEDBACK_REQUEST_NOTIFICATION =
            "service_homework_feedback_request_notification";
    private static final String SERVICE_HOMEWORK_FEEDBACK_NOTIFICATION =
            "service_homework_feedback_notification";
    private static final String SERVICE_HOMEWORK_ACCEPTED_AUTO = "service_homework_accepted_auto";
    private static final String SERVICE_FEEDBACK_FOR_HOMEWORK_WAITING =
            "service_feedback_for_homework_waiting";
    private static final String SERVICE_FEEDBACK_MEDIA_GROUP_BYPASS =
            "service_feedback_media_group_bypass";
    private static final String SERVICE_SEND_HOMEWORK_MEDIA_GROUP_BYPASS =
            "service_send_homework_media_group_bypass";

    private static final String ERROR_HOMEWORK_ALREADY_COMPLETED =
            "error_homework_already_completed";
    private static final String ERROR_HOMEWORK_ALREADY_AWAITS_APPROVAL =
            "error_homework_already_awaits_approval";
    private static final String ERROR_HOMEWORK_PROGRESS_NOT_FOUND =
            "error_homework_progress_not_found";
    private static final String ERROR_HOMEWORK_NOT_FOUND = "error_homework_not_found";
    private static final String ERROR_BOT_VISIBILITY_MISMATCH = "error_bot_visibility_mismatch";

    private static final String SEND_HOMEWORK_MENU_TERMINATION =
            "homework_progress_%s_send_homework_menus";
    private static final String FEEDBACK_MENU_TERMINATION =
            "homework_progress_%s_feedback_menus";
    private static final String MENTION = "@";

    private final HomeworkProgressRepository homeworkProgressRepository;

    private final CourseProgressRepository courseProgressRepository;

    private final HomeworkRepository homeworkRepository;

    private final ContentMappingRepository contentMappingRepository;

    private final MenuService menuService;

    private final UserService userService;

    private final ContentService contentService;

    private final LessonService lessonService;

    private final TimingService timingService;

    private final LocalizationLoader localizationLoader;
    
    private final ClientManager clientManager;

    @Lazy
    @Autowired
    private CourseService courseService;

    @Override
    public void initiateHomework(@NonNull UserEntity user, @NonNull Homework homework) {
        courseService.checkCourseIsNotUnderMaintenance(homework.getLesson().getCourse(), user);

        final Optional<HomeworkProgress> potentialProgress = homeworkProgressRepository
                .findByUserIdAndHomeworkIdUnresolved(user.getId(), homework.getId());

        HomeworkProgress homeworkProgress;
        if (potentialProgress.isEmpty()) {
            LOGGER.debug("User " + user.getId() + " does not have any "
                    + "unresolved homeworks. Creating new homework progress...");
            homeworkProgress = new HomeworkProgress();
            homeworkProgress.setUser(user);
            homeworkProgress.setHomework(homework);
            homeworkProgress.setStatus(Status.INITIALIZED);
            homeworkProgress.setInitializedAt(LocalDateTime.now());
            homeworkProgressRepository.save(homeworkProgress);
            LOGGER.debug("Homework progress created and persisted.");
        } else {
            LOGGER.debug("User " + user.getId() + " already has an unresolved homework progress.");
            homeworkProgress = potentialProgress.get();
        }
        final Optional<HomeworkTrigger> potentialHomeworkTrigger = timingService
                .findHomeworkTrigger(user, homework);
        if (potentialHomeworkTrigger.isPresent()) {
            LOGGER.debug("User " + user.getId() + " is currently awaiting homework.");
            return;
        }

        if (homework.getDelay() > 0) {
            LOGGER.debug("Homework " + homework.getId() + " has a delay of "
                    + homework.getDelay() + " minutes. Creating homework trigger...");
            final TimedTrigger trigger = timingService.createTrigger(homeworkProgress);
            LOGGER.debug("New trigger " + trigger.getId() + " for user " + user.getId()
                    + " and homework " + homework.getId() + " has been created. It will "
                    + "activate at " + trigger.getTarget() + ".");
            return;
        }
        sendHomework(homeworkProgress);
    }

    @Override
    public void sendHomework(@NonNull HomeworkProgress progress) {
        final UserEntity user = progress.getUser();
        final Homework homework = progress.getHomework();
        final Bot bot = homework.getLesson().getCourse().getBot();

        LOGGER.debug("Sending homework " + homework.getId() + " content to user "
                + user.getId() + "...");
        final List<Message> sendContent = contentService.sendLocalizedContent(contentService
                .getMappingById(homework.getMapping().getId(), user), user, bot);
        LOGGER.debug("Content has been sent.");
        Localization errorLocalization;
        switch (progress.getStatus()) {
            case AWAITS_APPROVAL:
                LOGGER.debug("User " + user.getId() + " is currently awaiting feedback for "
                        + "homework " + homework.getId() + ".");
                errorLocalization = localizationLoader
                        .getLocalizationForUser(ERROR_HOMEWORK_ALREADY_AWAITS_APPROVAL,
                        user);
                clientManager.getClient(bot).sendMessage(user, errorLocalization);
                return;
            case COMPLETED:
                if (homework.isRepeatedCompletionAvailable()) {
                    LOGGER.warn("User " + user.getId() + " has already completed homework "
                            + homework.getId() + " but since it supports repeated homework "
                            + "completions, they will be able to send it again. This can be "
                            + "disabled in course settings.");
                    break;
                }
                LOGGER.debug("User " + user.getId() + " has already completed homework "
                        + homework.getId() + ". Triggering next stage menu...");

                final Course course = homework.getLesson().getCourse();
                final CourseProgress courseProgress = courseService
                        .getCurrentCourseProgressForUser(user.getId(), course.getName());
                if (courseProgress.getStage().equals(course.getAmountOfLessons() - 1)) {
                    LOGGER.info("User " + user.getId() + " has completed course "
                            + course.getName() + ". Commencing ending sequence...");
                    courseService.end(user, courseProgress);
                    return;
                }
                errorLocalization = localizationLoader
                        .getLocalizationForUser(ERROR_HOMEWORK_ALREADY_COMPLETED, user);
                final Message message = clientManager.getClient(bot)
                        .sendMessage(user, errorLocalization);
                menuService.initiateMenu(COURSE_NEXT_STAGE_MENU, user, course.getName()
                        + CourseService.COURSE_NAME_LESSON_INDEX_DIVIDER + courseProgress.getStage(),
                        message.getMessageId(), bot);
                menuService.addToMenuTerminationGroup(user, user, bot, message.getMessageId(),
                        CourseService.COURSE_NEXT_STAGE_MENU_TERMINATION
                        .formatted(courseProgress.getId()), null);
                return;
            default:
        }
        final Message menuMessage;
        if (sendContent.size() > 1) {
            LOGGER.warn("Content in homework " + homework.getId() + " is a media group. "
                    + "It is a recomendation to avoid such cases since it requires an "
                    + "additional message to be sent for menu.");
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .getLocalizationForUser(SERVICE_SEND_HOMEWORK_MEDIA_GROUP_BYPASS, user);
            menuMessage = clientManager.getClient(bot).sendMessage(user, mediaGroupBypassMessageLoc);
            LOGGER.debug("Additional message for menu has been sent.");
        } else {
            LOGGER.debug("Content in homework " + homework.getId() + " is not a media group. "
                    + "Menu will be attached to it.");    
            menuMessage = sendContent.get(0);
        }
        menuService.initiateMenu(SEND_HOMEWORK_MENU, user, progress.getId().toString(),
                menuMessage.getMessageId(), bot);
        menuService.addToMenuTerminationGroup(user, user, bot, menuMessage.getMessageId(),
                SEND_HOMEWORK_MENU_TERMINATION.formatted(progress.getId()), null);
    }

    @Override
    public void commit(@NonNull HomeworkProgress progress, @NonNull List<Message> messages) {
        final Bot bot = progress.getHomework().getLesson().getCourse().getBot();

        progress.setContent(contentService.parseAndPersistContent(bot, messages,
                contentService.parseMediaTypes((progress.getHomework()
                .getAllowedMediaTypes() != null) ? progress.getHomework()
                .getAllowedMediaTypes() : "")));

        Localization localization;
        if (progress.getHomework().getLesson().getCourse().isFeedbackIncluded()
                && progress.getHomework().isFeedbackRequired()
                && requestFeedback(progress)) {
            progress.setStatus(Status.AWAITS_APPROVAL);
            progress.setApproveRequestedAt(LocalDateTime.now());
            localization = localizationLoader.getLocalizationForUser(
                    SERVICE_FEEDBACK_FOR_HOMEWORK_WAITING, progress.getUser());
            clientManager.getClient(bot).sendMessage(progress.getUser(), localization);
        } else {
            progress.setStatus(Status.COMPLETED);
            localization = localizationLoader.getLocalizationForUser(
                SERVICE_HOMEWORK_ACCEPTED_AUTO, progress.getUser());

            final List<BotRole> homeworkBotRoles = userService.getHomeworkBotRoles(bot);
            homeworkBotRoles.forEach(br -> {
                final Localization adminNotification = localizationLoader.getLocalizationForUser(
                        SERVICE_HOMEWORK_FEEDBACK_NOTIFICATION, br.getUser(),
                        getParameterMapForUserAndCourseInfo(progress));
                clientManager.getClient(bot).sendMessage(br.getUser(), adminNotification);
                contentService.sendContent(progress.getContent(), br.getUser(), bot);
            });
            clientManager.getClient(bot).sendMessage(progress.getUser(), localization);
            courseService.next(progress.getUser(), progress.getHomework()
                    .getLesson().getCourse().getName());
                    progress.setFinishedAt(LocalDateTime.now());
        }
        menuService.terminateMenuGroup(progress.getUser(), bot,
                SEND_HOMEWORK_MENU_TERMINATION.formatted(progress.getId()));
        homeworkProgressRepository.save(progress);
    }

    @Override
    public boolean requestFeedback(@NonNull HomeworkProgress homeworkProgress) {
        final Bot bot = homeworkProgress.getHomework().getLesson().getCourse().getBot();
        final List<BotRole> homeworkBotRoles = userService.getHomeworkBotRoles(bot);
        
        LOGGER.debug("Checking if there are any users who are receiving "
                + "homework feedback requests...");
        if (homeworkBotRoles.isEmpty()) {
            LOGGER.warn("There are no users who are receiving homework feedback in bot "
                    + bot.getName() + ". This means homework inclusion in course settings "
                    + "will be ignored.");
            return false;
        }
        for (BotRole botRole : homeworkBotRoles) {
            final UserEntity mentor = botRole.getUser();
            LOGGER.debug("User " + mentor.getId() + " has homework feedback enabled. "
                    + "Sending approval message to them...");
            final Localization localization = localizationLoader.getLocalizationForUser(
                    SERVICE_HOMEWORK_FEEDBACK_REQUEST_NOTIFICATION, mentor,
                    getParameterMapForUserAndCourseInfo(homeworkProgress));
            clientManager.getClient(bot).sendMessage(mentor, localization);
            LOGGER.debug("Homework feedback info has been sent to user " + mentor.getId() + ".");
            final List<Message> sendContent = contentService.sendContent(homeworkProgress
                    .getContent(), mentor, bot);
            LOGGER.debug("Homework content has been sent to user " + mentor.getId() + ".");
            final Message menuMessage;
        if (sendContent.size() > 1) {
            LOGGER.debug("Homework progress " + homeworkProgress.getId()
                    + "'s content is a media group. To avoid Telegram restrictions, an "
                    + "additional message will be sent to user " + mentor.getId()
                    + " to attach the feedback menu to.");
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .getLocalizationForUser(SERVICE_FEEDBACK_MEDIA_GROUP_BYPASS, mentor);
            menuMessage = clientManager.getClient(bot).sendMessage(mentor,
                    mediaGroupBypassMessageLoc);
            LOGGER.debug("Additional message for menu has been sent.");
        } else {
            LOGGER.debug("Homework progress " + homeworkProgress.getId() + "'s content "
                    + "is not a media group. Menu will be attached to it.");    
            menuMessage = sendContent.get(0);
        }
            menuService.initiateMenu(REQUEST_FEEDBACK_MENU, mentor,
                    homeworkProgress.getId().toString(), menuMessage.getMessageId(), bot);
            menuService.addToMenuTerminationGroup(homeworkProgress.getUser(), mentor, bot,
                    menuMessage.getMessageId(), FEEDBACK_MENU_TERMINATION.formatted(
                    homeworkProgress.getId()), null);
            LOGGER.debug("Feedback menu has been initialized for user " + mentor.getId() + ".");
        }
        return true;
    }

    @Override
    public void approve(@NonNull HomeworkProgress progress, @NonNull UserEntity user,
            @Nullable List<Message> adminComment) {
        final Bot bot = progress.getHomework().getLesson().getCourse().getBot();

        if (!progress.getStatus().equals(Status.COMPLETED) &&
                !progress.getStatus().equals(Status.DECLINED)) {
            Content adminCommentContent = null;
            if (adminComment != null) {
                adminCommentContent = contentService.parseAndPersistContent(bot, adminComment);
            }

            progress.setCurator(user);
            progress.setStatus(Status.COMPLETED);
            homeworkProgressRepository.save(progress);
    
            menuService.terminateMenuGroup(progress.getUser(), bot,
                    FEEDBACK_MENU_TERMINATION.formatted(progress.getId()));

            sendHomeworkNotification(progress, (adminCommentContent != null)
                    ? SERVICE_HOMEWORK_APPROVED_NOTIFICATION_PLUS_COMMENT
                    : SERVICE_HOMEWORK_APPROVED_NOTIFICATION);

            if (adminCommentContent != null) {
                contentService.sendContent(adminCommentContent, progress.getUser(), bot);
            }
            progress.setFinishedAt(LocalDateTime.now());

            courseService.next(progress.getUser(), progress.getHomework()
                    .getLesson().getCourse().getName());
        }
    }

    @Override
    public void decline(@NonNull HomeworkProgress progress, @NonNull UserEntity user,
            @NonNull List<Message> adminComment) {
        final Bot bot = progress.getHomework().getLesson().getCourse().getBot();
        final Content adminCommentContent = contentService
                .parseAndPersistContent(bot, adminComment);

        if (!progress.getStatus().equals(Status.COMPLETED) &&
                !progress.getStatus().equals(Status.DECLINED)) {
            progress.setCurator(user);
            progress.setStatus(Status.DECLINED);
            homeworkProgressRepository.save(progress);
    
            menuService.terminateMenuGroup(progress.getUser(), bot,
                    FEEDBACK_MENU_TERMINATION.formatted(progress.getId()));

            sendHomeworkNotification(progress, 
                    SERVICE_HOMEWORK_DECLINED_NOTIFICATION_PLUS_COMMENT);

            contentService.sendContent(adminCommentContent, progress.getUser(), bot);

            initiateHomework(progress.getUser(), progress.getHomework());
        }
    }

    @Override
    @NonNull
    public Homework getHomework(@NonNull Long id, @NonNull UserEntity user, @NonNull Bot bot) {
        final Homework homework = homeworkRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Homework with id " + id + " does not exist",
                localizationLoader.getLocalizationForUser(ERROR_HOMEWORK_NOT_FOUND, user)));
        if (!homework.getLesson().getCourse().getBot().equals(bot)) {
            throw new AccessDeniedException("Homework " + id + " is not available for bot "
                    + bot.getName(), localizationLoader.getLocalizationForUser(
                    ERROR_BOT_VISIBILITY_MISMATCH, user));
        }
        return homework;
    }

    @Override
    @NonNull
    public Homework save(@NonNull Homework homework) {
        return homeworkRepository.save(homework);
    }

    @Override
    @NonNull
    public ContentMapping updateContent(@NonNull Long homeworkId,
            @NonNull LocalizedContent content, @NonNull UserEntity user, @NonNull Bot bot) {
        final Homework homework = getHomework(homeworkId, user, bot);
        final ContentMapping contentMapping = new ContentMapping();

        contentMapping.setPosition(0);
        contentMapping.setContent(List.of(content));
        contentMapping.setTextEnabled(true);
        homework.setMapping(contentMappingRepository.save(contentMapping));
        homeworkRepository.save(homework);
        return contentMapping;
    }

    @Override
    @NonNull
    public Homework createDefault(@NonNull Lesson lesson, @NonNull LocalizedContent content) {
        LOGGER.debug("Lesson does not have a homework yet. Creating...");
        final Homework homework = new Homework();
        final ContentMapping mapping = new ContentMapping();
        mapping.setContent(List.of(content));
        mapping.setPosition(0);
        mapping.setTextEnabled(true);

        homework.setAllowedMediaTypes("");
        homework.setFeedbackRequired(true);
        homework.setLesson(lesson);
        homework.setDelay(0);
        homework.setMapping(contentMappingRepository.save(mapping));
        homework.setRepeatedCompletionAvailable(false);
        save(homework);
        lesson.setHomework(homework);
        lessonService.save(lesson);
        LOGGER.debug("New homework " + homework.getId() + " has been created.");
        return homework;
    }

    @Override
    @NonNull
    public HomeworkProgress getProgress(@NonNull Long id, @NonNull UserEntity user) {
        return homeworkProgressRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Homework progress with id " + id + " does not exist",
                localizationLoader.getLocalizationForUser(ERROR_HOMEWORK_PROGRESS_NOT_FOUND,
                user)));
    }

    private void sendHomeworkNotification(HomeworkProgress progress, String localizationName) {
        final Course course = progress.getHomework().getLesson().getCourse();
        final Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(PARAM_COURSE_NAME, course.getName());

        final CourseProgress courseProgress = courseProgressRepository
                .findByUserIdAndCourseName(progress.getUser().getId(),
                course.getName()).orElseThrow(() -> new EntityNotFoundException(
                "Course progress for course " + course.getName() + " and user "
                + progress.getUser().getId() + " does not exist", localizationLoader
                .getLocalizationForUser(ERROR_HOMEWORK_PROGRESS_NOT_FOUND,
                userService.getDiretor())));

        parameterMap.put(PARAM_LESSON_INDEX, courseProgress.getStage());
        parameterMap.put(PARAM_COMMENTER_NAME, progress.getCurator().getFullName());
        parameterMap.put(PARAM_TITLE, userService.getLocalizedTitle(progress.getCurator(),
                courseProgress.getCourse().getBot()));

        final Localization notification = localizationLoader
                .getLocalizationForUser(localizationName, progress.getUser(), parameterMap);
        
        clientManager.getClient(course.getBot()).sendMessage(progress.getUser(), notification);
    }

    private Map<String, Object> getParameterMapForUserAndCourseInfo(
            HomeworkProgress homeworkProgress) {
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_TARGET_ID, homeworkProgress.getUser().getId());
        parameterMap.put(PARAM_TARGET_FIRST_NAME, homeworkProgress.getUser().getFirstName());
        parameterMap.put(PARAM_TARGET_LAST_NAME,
                (homeworkProgress.getUser().getLastName() != null) ? homeworkProgress
                .getUser().getLastName() : "Not available");
        parameterMap.put(PARAM_TARGET_USERNAME,
                (homeworkProgress.getUser().getUsername() != null) ? MENTION + homeworkProgress
                .getUser().getUsername() : "Not available");
        parameterMap.put(PARAM_TARGET_LANGUAGE_CODE, homeworkProgress
                .getUser().getLanguageCode());

        parameterMap.put(PARAM_COURSE_NAME, homeworkProgress.getHomework()
                .getLesson().getCourse().getName());

        parameterMap.put(PARAM_LESSON_INDEX, homeworkProgress.getHomework()
                .getLesson().getPosition());
        return parameterMap;
    }
}
