package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
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

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Homework homework = homeworkService.getHomework(
                Long.parseLong(params[3]), user, bot);
        LOGGER.info("User " + user.getId() + " is trying to toggle feedback "
                + "for homework " + homework.getId() + ". Current status is "
                + getFeedbackStatus(homework) + ".");
        
        homework.setFeedbackRequired(!homework.isFeedbackRequired());
        homeworkService.save(homework);

        LOGGER.info("Feedback for homework " + homework.getId() + " is now "
                + getFeedbackStatus(homework) + ".");
        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_HOMEWORK_FEEDBACK_UPDATE_SUCCESS,
                user, PARAM_STATUS, getFeedbackStatus(user, homework)));
        LOGGER.debug("Message sent.");
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
