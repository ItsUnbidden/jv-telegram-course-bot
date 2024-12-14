package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class AddMappingLocalizationButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            AddMappingLocalizationButtonHandler.class);
    
    private static final String PARAM_CONTENT_ID = "${contentId}";
    private static final String PARAM_MAPPING_ID = "${mappingId}";
    private static final String PARAM_LANGUAGE_CODE = "${languageCode}";

    private static final String SERVICE_ADD_NEW_LOCALIZATION_REQUEST =
            "service_add_new_localization_request";
    private static final String SERVICE_ADD_NEW_LOCALIZATION_SUCCESS =
            "service_add_new_localization_success";

    private static final String ERROR_LANGUAGE_CODE_LENGTH = "error_language_code_length";
    private static final String ERROR_LOCALIZED_CONTENT_IS_ALREADY_PRESENT =
            "error_localized_content_is_already_present";

    private final ContentService contentService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final ContentMapping mapping = contentService
                .getMappingById(Long.parseLong(params[0]), user);

        LOGGER.info("User " + user.getId() + " is trying to add a new "
                + "localization to mapping " + mapping.getId() + ".");
        sessionService.createSession(user, bot, m -> {
            final Message lastMessage = m.get(m.size() - 1);
            final String languageCode;

            if (lastMessage.hasText()) {
                if (lastMessage.getText().length() > 3
                        || lastMessage.getText().length() < 2) {
                    throw new InvalidDataSentException("Language code must be "
                            + "between 2 and 3 characters", localizationLoader
                            .getLocalizationForUser(ERROR_LANGUAGE_CODE_LENGTH, user));
                }
                LOGGER.debug("Language code for new content will be "
                        + lastMessage.getText() + ".");
                languageCode = lastMessage.getText().trim();
            } else {
                languageCode = user.getLanguageCode();
                LOGGER.debug("Seems like language code is not specified. "
                        + "Assuming it to be user's telegram language which is "
                        + languageCode + ".");
            }
            if (mapping.getContent().stream()
                    .anyMatch(c -> c.getLanguageCode().equals(languageCode))) {
                final Map<String, Object> parameterMap = new HashMap<>();
                parameterMap.put(PARAM_MAPPING_ID, mapping.getId());
                parameterMap.put(PARAM_LANGUAGE_CODE, languageCode);

                throw new InvalidDataSentException("Localization with language code "
                        + languageCode + " is already present in mapping " + mapping.getId(),
                        localizationLoader.getLocalizationForUser(
                        ERROR_LOCALIZED_CONTENT_IS_ALREADY_PRESENT, user, parameterMap));
            }
            final LocalizedContent newContent = contentService.parseAndPersistContent(bot, m,
                    mapping.getContent().get(0).getData().getData(), languageCode);
            contentService.addNewLocalization(mapping, newContent, bot);
            LOGGER.info("New content " + newContent.getId() + " has been added to mapping "
                    + mapping.getId() + ".");
            LOGGER.debug("Sending confirmation message...");

            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_MAPPING_ID, mapping.getId());
            parameterMap.put(PARAM_CONTENT_ID, newContent.getId());

            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_ADD_NEW_LOCALIZATION_SUCCESS,
                    user, parameterMap));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending new localization request...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_ADD_NEW_LOCALIZATION_REQUEST, user,
                PARAM_MAPPING_ID, mapping.getId()));
        LOGGER.debug("Message sent.");
    }
}
