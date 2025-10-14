package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReceiveHomeworkToggleButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
        HomeworkFeedbackToggleButtonHandler.class);

            private static final String PARAM_STATUS = "${status}";

    private static final String SERVICE_TOGGLE_RECEIVE_HOMEWORK =
            "service_toggle_receive_homework";
    private static final String SERVICE_STATUS_DISABLED = "service_status_disabled";
    private static final String SERVICE_STATUS_ENABLED = "service_status_enabled";

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.GIVE_HOMEWORK_FEEDBACK)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        LOGGER.debug("User " + user.getId() + " is trying to toggle receive homework.");
        final BotRole botRole = userService.toggleReceiveHomework(user, bot);
        LOGGER.debug("Receive homework for user " + user.getId() + " is now "
                + getFeedbackStatus(botRole) + ".");

        LOGGER.debug("Sending confirmation message...");
        final Localization success = localizationLoader.getLocalizationForUser(
                SERVICE_TOGGLE_RECEIVE_HOMEWORK, user, PARAM_STATUS, getFeedbackStatus(user, botRole));

        clientManager.getClient(bot).sendMessage(user, success);
        LOGGER.debug("Message sent.");
    }

    private String getFeedbackStatus(UserEntity user, BotRole botRole) {
        return (botRole.isReceivingHomework()) ? localizationLoader
                .getLocalizationForUser(SERVICE_STATUS_ENABLED, user).getData()
                : localizationLoader.getLocalizationForUser(SERVICE_STATUS_DISABLED, user)
                .getData();
    }

    private String getFeedbackStatus(BotRole botRole) {
        return (botRole.isReceivingHomework()) ? "ENABLED" : "DISABLED";
    }
}
