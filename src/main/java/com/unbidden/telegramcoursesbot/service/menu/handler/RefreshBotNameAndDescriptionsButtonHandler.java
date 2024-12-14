package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.bot.RegularClient;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshBotNameAndDescriptionsButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            RefreshBotNameAndDescriptionsButtonHandler.class);

    private static final String SERVICE_NAMES_DESCRIPTIONS_REFRESH_SUCCESS =
            "service_names_descriptions_refresh_success";
            
    private final BotService botService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        botService.checkBotFather(bot, user);
        LOGGER.info("The director is trying to refresh bot names and descriptions...");
        final List<Bot> bots = botService.getAllBots();

        for (Bot botFromDb : bots) {
            final RegularClient client = (RegularClient)clientManager.getClient(botFromDb);
            LOGGER.debug("Setting descriptions for bot " + botFromDb.getName() + "...");
            client.setUpDescriptions();

            LOGGER.debug("Setting names for bot " + botFromDb.getName() + "...");
            client.setUpNames();

            LOGGER.debug("Setting short descriptions for bot " + botFromDb.getName() + "...");
            client.setUpShortDescriptions();
        }
        LOGGER.info("Names and descriptions have been refreshed.");
        LOGGER.debug("Sending confirmation message...");
        clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_NAMES_DESCRIPTIONS_REFRESH_SUCCESS, user));
        LOGGER.debug("Message sent.");
    }
}
