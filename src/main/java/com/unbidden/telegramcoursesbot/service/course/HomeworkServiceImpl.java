package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress.Status;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkProgressRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import jakarta.persistence.EntityNotFoundException;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
@RequiredArgsConstructor
public class HomeworkServiceImpl implements HomeworkService {
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

    private static final String ERROR_HOMEWORK_ALREADY_COMPLETED =
            "error_homework_already_completed";
    private static final String ERROR_HOMEWORK_ALREADY_AWAITS_APPROVAL =
            "error_homework_already_awaits_approval";

    private static final String SEND_HOMEWORK_MENU_TERMINATION =
            "homework_progress_%s_send_homework_menus";
    private static final String FEEDBACK_MENU_TERMINATION =
            "homework_progress_%s_feedback_menus";

    private static final Logger LOGGER = LogManager.getLogger(HomeworkServiceImpl.class);

    private final HomeworkProgressRepository homeworkProgressRepository;

    private final CourseProgressRepository courseProgressRepository;

    private final HomeworkRepository homeworkRepository;

    private final MenuService menuService;

    private final UserService userService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;
    
    private final TelegramBot bot;

    @Lazy
    @Autowired
    private CourseService courseService;

    @Override
    public void sendHomework(@NonNull UserEntity user, @NonNull Homework homework) {
        LOGGER.info("Sending homework " + homework.getId() + " content to user "
                + user.getId() + "...");
        final List<Message> sendContent = contentService.sendContent(homework.getContent(), user);
        LOGGER.info("Content has been sent.");

        final Optional<HomeworkProgress> potentialProgress = homeworkProgressRepository
                .findByUserIdAndHomeworkIdUnresolved(user.getId(), homework.getId());

        HomeworkProgress homeworkProgress;
        if (potentialProgress.isEmpty()) {
            LOGGER.info("User " + user.getId() + " does not have any "
                    + "unresolved homeworks. Creating new homework progress...");
            homeworkProgress = new HomeworkProgress();
            homeworkProgress.setUser(user);
            homeworkProgress.setHomework(homework);
            homeworkProgress.setStatus(Status.INITIALIZED);
            homeworkProgress.setInitializedAt(LocalDateTime.now());
            homeworkProgressRepository.save(homeworkProgress);
            LOGGER.info("Homework progress created and persisted.");
        } else {
            LOGGER.info("User " + user.getId() + " already has an unresolved homework progress.");
            homeworkProgress = potentialProgress.get();
        }
        Localization errorLocalization;
        switch (homeworkProgress.getStatus()) {
            case AWAITS_APPROVAL:
                LOGGER.info("User " + user.getId() + " is currently awaiting feedback for "
                        + "homework " + homework.getId() + ".");
                errorLocalization = localizationLoader
                        .getLocalizationForUser(ERROR_HOMEWORK_ALREADY_AWAITS_APPROVAL,
                        user);
                bot.sendMessage(SendMessage.builder()
                        .chatId(user.getId())
                        .text(errorLocalization.getData())
                        .entities(errorLocalization.getEntities())
                        .build());
                break;
            case COMPLETED:
                // TODO: potentially make this an optional thing depending on course settings.
                LOGGER.info("User " + user.getId() + " has already completed homework "
                        + homework.getId() + ". Triggering next stage menu...");
                
                errorLocalization = localizationLoader
                        .getLocalizationForUser(ERROR_HOMEWORK_ALREADY_COMPLETED, user);
                final Message message = bot.sendMessage(SendMessage.builder()
                        .chatId(user.getId())
                        .text(errorLocalization.getData())
                        .entities(errorLocalization.getEntities())
                        .build());

                final Course course = homework.getLesson().getCourse();
                final CourseProgress courseProgress = courseService
                        .getCurrentCourseProgressForUser(user.getId(), course.getName());

                menuService.initiateMenu(COURSE_NEXT_STAGE_MENU, user, course.getName()
                        + CourseService.COURSE_NAME_LESSON_INDEX_DIVIDER + courseProgress.getStage(),
                        message.getMessageId());
                menuService.addToMenuTerminationGroup(user, user, message.getMessageId(),
                        CourseService.COURSE_NEXT_STAGE_MENU_TERMINATION
                        .formatted(courseProgress.getId()), null);
                break;
            default:
                menuService.initiateMenu(SEND_HOMEWORK_MENU, user, homeworkProgress.getId().toString(),
                        sendContent.get(0).getMessageId());
                menuService.addToMenuTerminationGroup(user, user, sendContent.get(0)
                        .getMessageId(), SEND_HOMEWORK_MENU_TERMINATION.formatted(
                        homeworkProgress.getId()), null);
                return;
        }
    }

    @Override
    public void commit(@NonNull Long id, @NonNull List<Message> messages) {
        final HomeworkProgress homeworkProgress = getHomeworkProgress(id);
        homeworkProgress.setContent(contentService.parseAndPersistContent(messages,
                contentService.parseMediaTypes(homeworkProgress.getHomework()
                .getAllowedMediaTypes())));

        Localization localization;
        if (homeworkProgress.getHomework().getLesson().getCourse().isFeedbackIncluded()
                && homeworkProgress.getHomework().isFeedbackRequired()) {
            homeworkProgress.setStatus(Status.AWAITS_APPROVAL);
            homeworkProgress.setApproveRequestedAt(LocalDateTime.now());
            requestFeedback(homeworkProgress);
            localization = localizationLoader.getLocalizationForUser(
                    SERVICE_FEEDBACK_FOR_HOMEWORK_WAITING, homeworkProgress.getUser());
            bot.sendMessage(SendMessage.builder()
                    .chatId(homeworkProgress.getUser().getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
        } else {
            homeworkProgress.setStatus(Status.COMPLETED);
            localization = localizationLoader.getLocalizationForUser(
                SERVICE_HOMEWORK_ACCEPTED_AUTO, homeworkProgress.getUser());

            final List<UserEntity> admins = userService.getHomeworkReveivingAdmins();
            admins.forEach(a -> {
                final Localization adminNotification = localizationLoader.getLocalizationForUser(
                        SERVICE_HOMEWORK_FEEDBACK_NOTIFICATION, a,
                        getParameterMapForUserAndCourseInfo(homeworkProgress));
                bot.sendMessage(SendMessage.builder()
                        .chatId(a.getId())
                        .text(adminNotification.getData())
                        .entities(adminNotification.getEntities())
                        .build());
                contentService.sendContent(homeworkProgress.getContent(), a);
            });
            bot.sendMessage(SendMessage.builder()
                    .chatId(homeworkProgress.getUser().getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
            courseService.next(homeworkProgress.getUser(), homeworkProgress.getHomework()
                    .getLesson().getCourse().getName());
        }
        menuService.terminateMenuGroup(homeworkProgress.getUser(), SEND_HOMEWORK_MENU_TERMINATION
                .formatted(homeworkProgress.getId()));
        homeworkProgress.setFinishedAt(LocalDateTime.now());
        homeworkProgressRepository.save(homeworkProgress);
    }

    @Override
    public void requestFeedback(@NonNull HomeworkProgress homeworkProgress) {
        final List<UserEntity> admins = userService.getHomeworkReveivingAdmins();
        
        for (UserEntity admin : admins) {
            final Localization localization = localizationLoader.getLocalizationForUser(
                    SERVICE_HOMEWORK_FEEDBACK_REQUEST_NOTIFICATION, admin,
                    getParameterMapForUserAndCourseInfo(homeworkProgress));
            bot.sendMessage(SendMessage.builder()
                    .chatId(admin.getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
            final List<Message> sendContent = contentService.sendContent(homeworkProgress
                    .getContent(), admin);
            menuService.initiateMenu(REQUEST_FEEDBACK_MENU, admin,
                    homeworkProgress.getId().toString(), sendContent.get(0).getMessageId());
            menuService.addToMenuTerminationGroup(homeworkProgress.getUser(), admin,
                    sendContent.get(0).getMessageId(),FEEDBACK_MENU_TERMINATION.formatted(
                    homeworkProgress.getId()), null);
        }
    }

    @Override
    public void approve(@NonNull Long id, @NonNull UserEntity user,
            @Nullable List<Message> adminComment) {
        final HomeworkProgress homeworkProgress = getHomeworkProgress(id);

        if (!homeworkProgress.getStatus().equals(Status.COMPLETED) &&
                !homeworkProgress.getStatus().equals(Status.DECLINED)) {
            Content adminCommentContent = null;
            if (adminComment != null) {
                adminCommentContent = contentService.parseAndPersistContent(adminComment);
            }

            homeworkProgress.setCurator(userService.getUser(user.getId()));
            homeworkProgress.setStatus(Status.COMPLETED);
            homeworkProgressRepository.save(homeworkProgress);
    
            menuService.terminateMenuGroup(homeworkProgress.getUser(),
                    FEEDBACK_MENU_TERMINATION.formatted(homeworkProgress.getId()));

            sendHomeworkNotification(homeworkProgress, (adminCommentContent != null)
                    ? SERVICE_HOMEWORK_APPROVED_NOTIFICATION_PLUS_COMMENT
                    : SERVICE_HOMEWORK_APPROVED_NOTIFICATION);

            if (adminCommentContent != null) {
                contentService.sendContent(adminCommentContent, homeworkProgress.getUser());
            }

            courseService.next(homeworkProgress.getUser(), homeworkProgress.getHomework()
                    .getLesson().getCourse().getName());
            // TODO: look into timed responses
        }
    }

    @Override
    public void decline(@NonNull Long id, @NonNull UserEntity user,
            @NonNull List<Message> adminComment) {
        final HomeworkProgress homeworkProgress = getHomeworkProgress(id);
        final Content adminCommentContent = contentService.parseAndPersistContent(adminComment);

        if (!homeworkProgress.getStatus().equals(Status.COMPLETED) &&
                !homeworkProgress.getStatus().equals(Status.DECLINED)) {
            homeworkProgress.setCurator(userService.getUser(user.getId()));
            homeworkProgress.setStatus(Status.DECLINED);
            homeworkProgressRepository.save(homeworkProgress);
    
            menuService.terminateMenuGroup(homeworkProgress.getUser(),
                    FEEDBACK_MENU_TERMINATION.formatted(homeworkProgress.getId()));

            sendHomeworkNotification(homeworkProgress, 
                    SERVICE_HOMEWORK_DECLINED_NOTIFICATION_PLUS_COMMENT);

            contentService.sendContent(adminCommentContent, homeworkProgress.getUser());

            sendHomework(homeworkProgress.getUser(), homeworkProgress.getHomework());
        }
    }

    @Override
    @NonNull
    public Homework getHomework(@NonNull Long id) {
        return homeworkRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(
                "Homework with id " + id + " does not exist."));
    }

    private HomeworkProgress getHomeworkProgress(Long id) {
        return homeworkProgressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Homework progress " + id
                + " does not exist."));
    }

    private void sendHomeworkNotification(HomeworkProgress progress, String localizationName) {
        final Course course = progress.getHomework().getLesson().getCourse();
        final Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(PARAM_COURSE_NAME, course.getName());

        final CourseProgress courseProgress = courseProgressRepository
                .findByUserIdAndCourseName(progress.getUser().getId(),
                course.getName()).orElseThrow(() -> new EntityNotFoundException(
                "Course progress for course " + course.getName() + " and user "
                + progress.getUser().getId() + " does not exist."));

        parameterMap.put(PARAM_LESSON_INDEX, courseProgress.getStage());
        parameterMap.put(PARAM_COMMENTER_NAME, progress.getCurator().getFirstName());

        final Localization notification = localizationLoader
                .getLocalizationForUser(localizationName, progress.getUser(), parameterMap);
        
        bot.sendMessage(SendMessage.builder()
                .chatId(progress.getUser().getId())
                .text(notification.getData())
                .entities(notification.getEntities())
                .build());
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
                (homeworkProgress.getUser().getUsername() != null) ? homeworkProgress
                .getUser().getUsername() : "Not available");
        parameterMap.put(PARAM_TARGET_LANGUAGE_CODE, homeworkProgress
                .getUser().getLanguageCode());

        parameterMap.put(PARAM_COURSE_NAME, homeworkProgress.getHomework()
                .getLesson().getCourse().getName());

        parameterMap.put(PARAM_LESSON_INDEX, homeworkProgress.getHomework()
                .getLesson().getIndex());
        return parameterMap;
    }
}
