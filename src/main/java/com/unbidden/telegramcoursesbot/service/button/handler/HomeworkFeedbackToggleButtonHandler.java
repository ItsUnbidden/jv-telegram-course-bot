package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeworkFeedbackToggleButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            HomeworkFeedbackToggleButtonHandler.class);

    private static final String PARAM_STATUS = "${status}";

    private static final String SERVICE_HOMEWORK_FEEDBACK_UPDATE_SUCCESS =
            "service_homework_feedback_update_success";
    private static final String SERVICE_STATUS_DISABLED = "service_status_disabled";
    private static final String SERVICE_STATUS_ENABLED = "service_status_enabled";

    private final HomeworkService homeworkService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Homework homework = homeworkService.getHomework(
                    Long.parseLong(params[3]), user);
            LOGGER.info("User " + user.getId() + " is trying to toggle feedback "
                    + "for homework " + homework.getId() + ". Current status is "
                    + getFeedbackStatus(homework) + ".");
            
            homework.setFeedbackRequired(!homework.isFeedbackRequired());
            homeworkService.save(homework);

            LOGGER.info("Feedback for homework " + homework.getId() + " is now "
                    + getFeedbackStatus(homework) + ".");
            LOGGER.debug("Sending confirmation message...");
            client.sendMessage(user, localizationLoader.getLocalizationForUser(
                    SERVICE_HOMEWORK_FEEDBACK_UPDATE_SUCCESS, user, PARAM_STATUS,
                    getFeedbackStatus(user, homework)));
            LOGGER.debug("Message sent.");
        }
    }

    private String getFeedbackStatus(UserEntity user, Homework homework) {
        return (homework.isFeedbackRequired()) ? localizationLoader
                .getLocalizationForUser(SERVICE_STATUS_ENABLED, user).getData()
                : localizationLoader.getLocalizationForUser(SERVICE_STATUS_DISABLED, user)
                .getData();
    }

    private String getFeedbackStatus(Homework homework) {
        return (homework.isFeedbackRequired()) ? "ENABLED" : "DISABLED";
    }
}
