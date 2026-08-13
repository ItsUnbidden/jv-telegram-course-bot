package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.bot.RegularClient;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshBotNameAndDescriptionsButtonHandler extends AbstractButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            RefreshBotNameAndDescriptionsButtonHandler.class);

    private static final String SERVICE_NAMES_DESCRIPTIONS_REFRESH_SUCCESS =
            "service_names_descriptions_refresh_success";
            
    private final BotService botService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        botService.checkBotLord(bot, user);
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
        clientManager.getBotLordClient().sendMessage(user, localizationLoader
                .localize(SERVICE_NAMES_DESCRIPTIONS_REFRESH_SUCCESS, user));
        LOGGER.debug("Message sent.");
    }
}
