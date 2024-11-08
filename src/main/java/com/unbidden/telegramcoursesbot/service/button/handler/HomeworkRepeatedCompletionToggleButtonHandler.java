package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
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
public class HomeworkRepeatedCompletionToggleButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            HomeworkRepeatedCompletionToggleButtonHandler.class);

    private static final String PARAM_STATUS = "${status}";

    private static final String SERVICE_REPEATED_COMPLETION_UPDATE_SUCCESS =
            "service_repeated_completion_update_success";
    private static final String SERVICE_STATUS_DISABLED = "service_status_disabled";
    private static final String SERVICE_STATUS_ENABLED = "service_status_enabled";

    private final HomeworkService homeworkService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Homework homework = homeworkService.getHomework(
                    Long.parseLong(params[3]), user);
            LOGGER.info("User " + user.getId() + " is trying to toggle repeated "
                    + "completion for homework " + homework.getId() + ". Current status is "
                    + getRepeatedCompletionStatus(homework) + ".");
            
            homework.setRepeatedCompletionAvailable(!homework.isRepeatedCompletionAvailable());
            homeworkService.save(homework);

            LOGGER.info("Repeated completion for homework " + homework.getId() + " is now "
                    + getRepeatedCompletionStatus(homework) + ".");
            LOGGER.debug("Sending confirmation message...");
            bot.sendMessage(user, localizationLoader.getLocalizationForUser(
                    SERVICE_REPEATED_COMPLETION_UPDATE_SUCCESS, user, PARAM_STATUS,
                    getRepeatedCompletionStatus(user, homework)));
            LOGGER.debug("Message sent.");
        }
    }

    private String getRepeatedCompletionStatus(UserEntity user, Homework homework) {
        return (homework.isRepeatedCompletionAvailable()) ? localizationLoader
                .getLocalizationForUser(SERVICE_STATUS_ENABLED, user).getData()
                : localizationLoader.getLocalizationForUser(SERVICE_STATUS_DISABLED, user)
                .getData();
    }

    private String getRepeatedCompletionStatus(Homework homework) {
        return (homework.isRepeatedCompletionAvailable()) ? "ENABLED" : "DISABLED";
    }
}
