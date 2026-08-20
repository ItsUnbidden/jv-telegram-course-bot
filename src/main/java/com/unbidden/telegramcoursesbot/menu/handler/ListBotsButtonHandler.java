package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotOrchestrationService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListBotsButtonHandler extends AbstractButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(ListBotsButtonHandler.class);

    private final BotOrchestrationService botService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.BOTS_SETTINGS, isBotLordOnly = true)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final StringBuilder builder = new StringBuilder();
        final List<BotRole> botRoles = botService.getAllCreatorRoles();
        
        LOGGER.debug("Compiling bots list...");
        for (final BotRole role : botRoles) {
            builder.append(role.getBot().getId()).append(" | ")
                    .append(role.getBot().getStart().getId()).append(" | ")
                    .append(role.getBot().getTerms().getId()).append(" | ")
                    .append(role.getBot().getCreatorInfo().getId()).append(" | ")
                    .append(role.getUser().getFullName()).append('\n');
        }
        builder.delete(builder.length() - 1, builder.length());

        LOGGER.debug("Bot list compiled. Sending...");
        clientManager.getBotLordClient().sendMessage(user, localizationLoader
                .localize(Localizations.Service.LIST_BOTS, user,
                    new Localizations.Service.ListBotsParams(builder.toString())));
        LOGGER.debug("Message sent.");
    }
}
