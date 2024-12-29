package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelectLanguageButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(SelectLanguageButtonHandler.class);

    private static final String SERVICE_LANGUAGE_MANUALLY_SET = "service_language_manually_set";
    private static final String SERVICE_LANGUAGE_RESET_TO_DEFAULT =
            "service_language_reset_to_default";

    private static final String DEFAULT_LANGUAGE_CODE = "dlc";

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.INFO)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        switch (params[0]) {
            case DEFAULT_LANGUAGE_CODE:
                userService.resetLanguageToDefault(user);
                LOGGER.debug("Sending confirmation message...");
                clientManager.getClient(bot).sendMessage(user, localizationLoader
                        .getLocalizationForUser(SERVICE_LANGUAGE_RESET_TO_DEFAULT, user));
                break;
            default:
                userService.changeLanguage(user, params[0]);
                LOGGER.debug("Sending confirmation message...");
                clientManager.getClient(bot).sendMessage(user, localizationLoader
                        .getLocalizationForUser(SERVICE_LANGUAGE_MANUALLY_SET, user));
                break;
        }
        LOGGER.debug("Message sent.");
    }
}
