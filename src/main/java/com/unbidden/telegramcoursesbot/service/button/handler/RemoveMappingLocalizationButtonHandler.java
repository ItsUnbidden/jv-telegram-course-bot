package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RemoveMappingLocalizationButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            RemoveMappingLocalizationButtonHandler.class);

    private static final String PARAM_LANGUAGE_CODE = "${languageCode}";
    private static final String PARAM_MAPPING_ID = "${mappingId}";

    private static final String SERVICE_REMOVE_LOCALIZATION_FROM_MAPPING_REQUEST =
            "service_remove_localization_from_mapping_request";

    private static final String SERVICE_REMOVE_LOCALIZATION_FROM_MAPPING_SUCCESS =
            "service_remove_localization_from_mapping_success";

    private final ContentService contentService;

    private final ContentSessionService sessionService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final ContentMapping mapping = contentService.getMappingById(
                    Long.parseLong(params[0]));
            LOGGER.debug("User " + user.getId() + " is trying to remove a "
                    + "localization from mapping " + mapping.getId() + ".");
            sessionService.createSession(user, m -> {
                if (m.size() != 1) {
                    throw new InvalidDataSentException("Only one message with language "
                            + "code was expected");
                }
                if (!m.get(0).hasText()) {
                    throw new InvalidDataSentException("Text message with "
                            + "language code was expected, but provided message "
                            + "does not have any text");
                }
                final String languageCode = m.get(0).getText().trim();
                if (languageCode.length() > 3 || languageCode.length() < 2) {
                    throw new InvalidDataSentException("Language code must be "
                            + "between 2 and 3 characters");
                }
                if (contentService.removeLocalization(mapping, languageCode)) {
                    LOGGER.info("Localization with code " + languageCode
                            + " has been removed from mapping " + mapping.getId() + ".");
                    LOGGER.debug("Sending confirmation message...");

                    final Map<String, Object> parameterMap = new HashMap<>();
                    parameterMap.put(PARAM_MAPPING_ID, mapping.getId());
                    parameterMap.put(PARAM_LANGUAGE_CODE, languageCode);

                    bot.sendMessage(user, localizationLoader.getLocalizationForUser(
                            SERVICE_REMOVE_LOCALIZATION_FROM_MAPPING_SUCCESS, user,
                            parameterMap));
                    LOGGER.debug("Message sent.");
                    return;
                }
                throw new InvalidDataSentException("No elements were deleted since there is no "
                        + "localization with language code " + languageCode);
            });
            LOGGER.debug("Sending language code request message...");
            bot.sendMessage(user, localizationLoader.getLocalizationForUser(
                    SERVICE_REMOVE_LOCALIZATION_FROM_MAPPING_REQUEST, user,
                    PARAM_MAPPING_ID, mapping.getId()));
            LOGGER.debug("Message sent.");
        }
    }
}
