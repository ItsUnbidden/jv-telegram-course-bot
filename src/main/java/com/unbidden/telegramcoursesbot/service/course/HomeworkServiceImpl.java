package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.Content;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.MessageEntity;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress.Status;
import com.unbidden.telegramcoursesbot.repository.ContentRepository;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkProgressRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkRepository;
import com.unbidden.telegramcoursesbot.repository.MessageRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
public class HomeworkServiceImpl implements HomeworkService {
    @Autowired
    private HomeworkProgressRepository homeworkProgressRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private MenuService menuService;

    @Autowired
    private UserService userService;

    @Autowired
    @Lazy
    private CourseService courseService;

    @Autowired
    private LocalizationLoader localizationLoader;

    @Autowired
    private TelegramBot bot;

    @Override
    public void sendHomework(@NonNull UserEntity user, @NonNull Homework homework) {
        final UserEntity userFromDb = userService.getUser(user.getId());

        bot.sendContent(contentRepository.findById(homework.getContent().getId()).get(), user);

        final Optional<HomeworkProgress> potentialProgress = homeworkProgressRepository
                .findByUserIdAndHomeworkIdUnresolved(userFromDb.getId(), homework.getId());

        HomeworkProgress homeworkProgress;
        if (potentialProgress.isEmpty()) {
            homeworkProgress = new HomeworkProgress();
            homeworkProgress.setUser(userFromDb);
            homeworkProgress.setHomework(homework);
            homeworkProgress.setStatus(Status.INITIALIZED);
            homeworkProgress.setApproveMessages(new ArrayList<>());
            homeworkProgress.setSendHomeworkMessages(new ArrayList<>());
            homeworkProgressRepository.save(homeworkProgress);
        } else {
            homeworkProgress = potentialProgress.get();
        }
        Localization errorLocalization;
        switch (homeworkProgress.getStatus()) {
            case AWAITS_APPROVAL:
                errorLocalization = localizationLoader
                        .getLocalizationForUser("error_homework_already_awaits_approval",
                        userFromDb);
                break;
            case DECLINED:
                errorLocalization = localizationLoader
                        .getLocalizationForUser("error_homework_already_declined", userFromDb);
                break;
            case COMPLETED:
                errorLocalization = localizationLoader
                        .getLocalizationForUser("error_homework_already_completed", userFromDb);
                break;
            default:
                final Message menuMessage = menuService.initiateMenu("m_sHw", user,
                homeworkProgress.getId().toString());
                MessageEntity menuMessageEntity = new MessageEntity();
                menuMessageEntity.setMessageId(menuMessage.getMessageId());
                menuMessageEntity.setUser(userFromDb);
                homeworkProgress.getSendHomeworkMessages().add(messageRepository
                        .save(menuMessageEntity));
                homeworkProgressRepository.save(homeworkProgress);
                return;
        }
        bot.sendMessage(SendMessage.builder()
                .chatId(userFromDb.getId())
                .text(errorLocalization.getData())
                .entities(errorLocalization.getEntities())
                .build());
    }

    @Override
    public void process(@NonNull Long id, @NonNull Message message) {
        final HomeworkProgress homeworkProgress = getHomeworkProgress(id);
        
        homeworkProgress.setContent(bot.parseAndPersistContent(message));
        homeworkProgress.setStatus(Status.CONTENT_SENT);
        homeworkProgressRepository.save(homeworkProgress);

        menuService.initiateMenu("m_csHw", homeworkProgress.getUser(),
                homeworkProgress.getId().toString());
    }

    @Override
    public void commit(@NonNull Long id) {
        final HomeworkProgress homeworkProgress = getHomeworkProgress(id);

        Localization localization;
        if (homeworkProgress.getHomework().getLesson().getCourse().isFeedbackIncluded()
                && homeworkProgress.getHomework().isFeedbackRequired()) {
            homeworkProgress.setStatus(Status.AWAITS_APPROVAL);
            requestFeedback(homeworkProgress);
            localization = localizationLoader.getLocalizationForUser(
                    "service_feedback_for_homework_waiting", homeworkProgress.getUser());
            bot.sendMessage(SendMessage.builder()
                    .chatId(homeworkProgress.getUser().getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
        } else {
            homeworkProgress.setStatus(Status.COMPLETED);
            localization = localizationLoader.getLocalizationForUser(
                "service_homework_accepted_auto", homeworkProgress.getUser());

            final List<UserEntity> admins = userService.getHomeworkReveivingAdmins();
            admins.forEach(a -> bot.sendContent(contentRepository.findById(homeworkProgress
                    .getContent().getId()).get(), a));
            bot.sendMessage(SendMessage.builder()
                    .chatId(homeworkProgress.getUser().getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
            courseService.next(homeworkProgress.getUser(), homeworkProgress.getHomework()
                    .getLesson().getCourse().getName());
        }
        for (MessageEntity message : homeworkProgress.getSendHomeworkMessages()) {
            menuService.terminateMenu(message.getUser().getId(), message.getMessageId(),
                    localizationLoader.getLocalizationForUser("service_homework_sent",
                    message.getUser()));
        }
        homeworkProgress.getSendHomeworkMessages().clear();
        homeworkProgressRepository.save(homeworkProgress);
    }

    @Override
    public void requestFeedback(@NonNull HomeworkProgress homeworkProgress) {
        final List<UserEntity> admins = userService.getHomeworkReveivingAdmins();
        
        for (UserEntity admin : admins) {
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("${targetId}", homeworkProgress.getUser().getId());
            parameterMap.put("${targetFirstName}", homeworkProgress.getUser().getFirstName());
            parameterMap.put("${targetLastName}",
                    (homeworkProgress.getUser().getLastName() != null) ? homeworkProgress
                    .getUser().getLastName() : "Not available");
            parameterMap.put("${targetUsername}",
                    (homeworkProgress.getUser().getUsername() != null) ? homeworkProgress
                    .getUser().getUsername() : "Not available");
            parameterMap.put("${targetLanguageCode}", homeworkProgress
                    .getUser().getLanguageCode());

            parameterMap.put("${courseName}", homeworkProgress.getHomework()
                    .getLesson().getCourse().getName());

            parameterMap.put("${lessonIndex}", homeworkProgress.getHomework()
                    .getLesson().getIndex());

            final Localization localization = localizationLoader.getLocalizationForUser(
                    "service_homework_feedback_request_notification", admin, parameterMap);
            bot.sendMessage(SendMessage.builder()
                    .chatId(admin.getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
            bot.sendContent(contentRepository.findById(homeworkProgress
                    .getContent().getId()).get(), admin);
            final Message menuMessage = menuService.initiateMenu("m_rqF", admin,
                    homeworkProgress.getId().toString());
            
            homeworkProgress.getApproveMessages().add(new MessageEntity(
                    admin, menuMessage.getMessageId()));
        }
        messageRepository.saveAll(homeworkProgress.getApproveMessages());
    }

    @Override
    public void approve(@NonNull Long id, @NonNull User user, @Nullable Message adminComment) {
        final HomeworkProgress homeworkProgress = getHomeworkProgress(id);

        if (!homeworkProgress.getStatus().equals(Status.COMPLETED) &&
                !homeworkProgress.getStatus().equals(Status.DECLINED)) {
            Content adminCommentContent = null;
            if (adminComment != null) {
                adminCommentContent = bot.parseAndPersistContent(adminComment);
            }

            homeworkProgress.setCurator(userService.getUser(user.getId()));
            homeworkProgress.setStatus(Status.COMPLETED);
            homeworkProgressRepository.save(homeworkProgress);
    
            for (MessageEntity message : homeworkProgress.getApproveMessages()) {
                final Localization success = localizationLoader
                        .getLocalizationForUser("service_homework_approved", message.getUser());
                menuService.terminateMenu(message.getUser().getId(), message.getMessageId(),
                        success);
            }

            sendHomeworkNotification(homeworkProgress, (adminCommentContent != null)
                    ? "service_homework_approved_notification_plus_comment"
                    : "service_homework_approved_notification");

            if (adminCommentContent != null) {
                bot.sendContent(adminCommentContent, homeworkProgress.getUser());
            }

            courseService.next(homeworkProgress.getUser(), homeworkProgress.getHomework()
                    .getLesson().getCourse().getName());
            // TODO: look into timed responses
        }
    }

    @Override
    public void decline(@NonNull Long id, @NonNull User user, @NonNull Message adminComment) {
        final HomeworkProgress homeworkProgress = getHomeworkProgress(id);
        final Content adminCommentContent = bot.parseAndPersistContent(adminComment);

        if (!homeworkProgress.getStatus().equals(Status.COMPLETED) &&
                !homeworkProgress.getStatus().equals(Status.DECLINED)) {
            homeworkProgress.setCurator(userService.getUser(user.getId()));
            homeworkProgress.setStatus(Status.DECLINED);
            homeworkProgressRepository.save(homeworkProgress);
    
            for (MessageEntity message : homeworkProgress.getApproveMessages()) {
                final Localization failure = localizationLoader
                        .getLocalizationForUser("service_homework_declined", message.getUser());
                menuService.terminateMenu(message.getUser().getId(), message.getMessageId(),
                        failure);
            }

            sendHomeworkNotification(homeworkProgress, 
                    "service_homework_declined_notification_plus_comment");

            bot.sendContent(adminCommentContent, homeworkProgress.getUser());

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

        parameterMap.put("${courseName}", course.getName());

        final CourseProgress courseProgress = courseProgressRepository
                .findByUserIdAndCourseName(progress.getUser().getId(),
                course.getName()).orElseThrow(() -> new EntityNotFoundException(
                "Course progress for course " + course.getName() + " and user "
                + progress.getUser().getId() + " does not exist."));

        parameterMap.put("${lessonIndex}", courseProgress.getStage());
        parameterMap.put("${commenterName}", progress.getCurator().getFirstName());

        final Localization notification = localizationLoader
                .getLocalizationForUser(localizationName, progress.getUser(), parameterMap);
        
        bot.sendMessage(SendMessage.builder()
                .chatId(progress.getUser().getId())
                .text(notification.getData())
                .entities(notification.getEntities())
                .build());
    }
}
