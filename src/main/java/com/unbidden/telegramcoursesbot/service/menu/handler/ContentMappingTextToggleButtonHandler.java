package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentMappingTextToggleButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            ContentMappingTextToggleButtonHandler.class);
        
    private static final String PARAM_STATUS = "${status}";
        
    private static final String SERVICE_STATUS_DISABLED = "service_status_disabled";
    private static final String SERVICE_STATUS_ENABLED = "service_status_enabled";
    private static final String SERVICE_MAPPING_TEXT_STATUS_UPDATE_SUCCESS =
            "service_mapping_text_status_update_success";

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final ContentMapping mapping = contentService
                .getMappingById(Long.parseLong(params[0]), user);
        LOGGER.info("User " + user.getId() + " is trying to toggle text "
                + "in mapping " + mapping.getId() + ". Current status is "
                + getTextStatus(mapping) + ".");
        
        mapping.setTextEnabled(!mapping.isTextEnabled());
        contentService.saveMapping(mapping);

        LOGGER.info("Text in mapping " + mapping.getId() + " is now "
                + getTextStatus(mapping) + ".");
        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_MAPPING_TEXT_STATUS_UPDATE_SUCCESS,
                user, PARAM_STATUS, getTextStatus(user, mapping)));
        LOGGER.debug("Message sent.");
    }

    private String getTextStatus(UserEntity user, ContentMapping mapping) {
        return (mapping.isTextEnabled()) ? localizationLoader
                .getLocalizationForUser(SERVICE_STATUS_ENABLED, user).getData()
                : localizationLoader.getLocalizationForUser(SERVICE_STATUS_DISABLED, user)
                .getData();
    }

    private String getTextStatus(ContentMapping mapping) {
        return (mapping.isTextEnabled()) ? "ENABLED" : "DISABLED";
    }
}
