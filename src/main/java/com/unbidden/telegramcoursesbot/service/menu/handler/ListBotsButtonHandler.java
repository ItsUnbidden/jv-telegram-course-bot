package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListBotsButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(ListBotsButtonHandler.class);

    private static final String PARAM_BOTS = "${bots}";

    private static final String SERVICE_LIST_BOTS = "service_list_bots";

    private final UserService userService;

    private final BotService botService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.BOTS_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        botService.checkBotFather(bot, user);
        final StringBuilder builder = new StringBuilder();
        final List<Bot> bots = botService.getAllBots();
        
        LOGGER.debug("Compiling bots list...");
        for (Bot botFromDb : bots) {
            builder.append(botFromDb.getId()).append(" | ")
                    .append(botFromDb.getName()).append(" | ")
                    .append(userService.getCreator(botFromDb).getFullName()).append('\n');
        }
        builder.delete(builder.length() - 1, builder.length());
        LOGGER.debug("Bot list compiled. Sending...");
        clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_LIST_BOTS, user, PARAM_BOTS, builder.toString()));
        LOGGER.debug("Message sent.");
    }
}
