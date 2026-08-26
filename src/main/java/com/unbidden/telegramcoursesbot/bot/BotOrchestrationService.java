package com.unbidden.telegramcoursesbot.bot;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BotOrchestrationService {
    private static final Logger LOGGER = LogManager.getLogger(BotOrchestrationService.class);

    private static final Pattern BOT_TOKEN_PATTERN = Pattern.compile("\\d{1,20}:[a-zA-Z0-9\\-]{1,50}");
    
    private final BotService botService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    public List<Bot> getRegularBots() {
        return botService.getRegularBots();
    }

    public List<BotRole> getAllCreatorRoles() {
        return botService.getAllCreatorRoles();
    }

    public Bot updateBotLord(UserEntity director) {
        return botService.updateBotLord(director);
    }

    public Bot updateInitialBot(UserEntity director) {
        return botService.updateInitialBot(director);
    }

    public void createBot(UserEntity director, Long creatorId, String token) {
        final String botToken = token.trim();

        if (!BOT_TOKEN_PATTERN.matcher(botToken).matches()) {
            throw new InvalidDataSentException("Bot token " + botToken
                    + " does not match the bot token  pattern", loader
                    .localize(Localizations.Error.BOT_TOKEN_PATTERN_MISMATCH, director));
        }
        LOGGER.debug("Bot token has been parsed.");

        LOGGER.info("Creating a new bot for creator " + creatorId + "...");
        final Bot newBot = botService.createBot(director, creatorId, botToken);

        LOGGER.info("New bot " + newBot.getId() + " has been created. Initializing...");

        clientManager.addClient(newBot);

        LOGGER.debug("Client initialized for the new bot " + newBot.getId() + ". Sending confirmation messages...");
        final UserEntity creator = entityUtil.getCreator(newBot);
        
        clientManager.getBotLordClient().sendMessage(director, loader.localize(Localizations.Service.NEW_BOT_CREATED, director));
        clientManager.getClient(newBot).sendMessage(creator, loader.localize(Localizations.Service.BOT_CREATED_CREATOR_NOTIFICATION, creator));
        LOGGER.debug("Messages sent.");
    }

    public List<Bot> initializeBots() {
        final List<Bot> bots = botService.getRegularBots();

        bots.forEach(b -> clientManager.addClient(b));
        return bots;
    }

    public Bot initializeBotLord(Bot bot) {
        clientManager.addBotLordClient(bot);

        return bot;
    }
}
