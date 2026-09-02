package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshLocalizationsButtonHandler extends AbstractButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(RefreshLocalizationsButtonHandler.class);

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE, isBotLordOnly = true)
    public void handle(BotRole botRole, Map<String, String> params) {
        if (!clientManager.isOnMaintenance()) {
            throw new ForbiddenOperationException("Unable to refresh because server is not on "
                    + "maintenance", localizationLoader.localize(Localizations.Error.MAINTENANCE_IN_NOT_ENABLED, botRole));
        }
        LOGGER.info("The director is trying to refresh localizations...");

        clientManager.setRefreshing(true);
        localizationLoader.reloadResourses();
        clientManager.setRefreshing(false);

        LOGGER.info("Localizations refresh has been completed.");
        
        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.LOCALIZATIONS_REFRESH_SUCCESS, botRole));
        LOGGER.debug("Message sent.");
    }
}
