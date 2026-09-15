package com.unbidden.telegramcoursesbot.bot;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

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

    private final ValidatorUtil validatorUtil;

    public List<Bot> getRegularBots() {
        return botService.getRegularBots();
    }

    public List<BotRole> getAllCreatorRoles() {
        return botService.getAllCreatorRoles();
    }

    public Bot updateBotLord(UserEntity director) {
        Assert.notNull(director, "director cannot be null");

        return botService.updateBotLord(director);
    }

    public Bot updateInitialBot(UserEntity director) {
        Assert.notNull(director, "director cannot be null");

        return botService.updateInitialBot(director);
    }

    public void createBot(BotRole botRole, Long creatorId, String token) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(creatorId, "creatorId cannot be null");
        Assert.notNull(token, "token cannot be null");

        final String botToken = token.trim();

        if (!BOT_TOKEN_PATTERN.matcher(botToken).matches()) {
            throw new InvalidDataSentException("Bot token " + botToken
                    + " does not match the bot token  pattern", loader
                    .localize(Localizations.Error.BOT_TOKEN_PATTERN_MISMATCH, botRole));
        }
        LOGGER.debug("Bot token has been parsed.");

        LOGGER.info("Creating a new bot for creator " + creatorId + "...");
        final Bot newBot = botService.createBot(botRole, creatorId, botToken);

        LOGGER.info("New bot " + newBot.getId() + " has been created. Initializing...");

        clientManager.addClient(newBot);

        LOGGER.debug("Client initialized for the new bot " + newBot.getId() + ". Sending confirmation messages...");
        final BotRole creatorRole = entityUtil.getCreator(newBot.getId());
        
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.NEW_BOT_CREATED, botRole));
        clientManager.sendMessage(creatorRole, loader.localize(Localizations.Service.BOT_CREATED_CREATOR_NOTIFICATION, creatorRole));
        LOGGER.debug("Messages sent.");
    }

    public List<Bot> initializeBots() {
        final List<Bot> bots = botService.getRegularBots();

        bots.forEach(b -> clientManager.addClient(b));
        return bots;
    }

    public Bot initializeBotLord(Bot bot) {
        Assert.notNull(bot, "bot cannot be null");

        clientManager.addBotLordClient(bot);

        return bot;
    }

    public void addCreatorInfo(BotRole botRole, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        validatorUtil.checkAtLeastExpectedMessages(botRole, messages, 1);
        String languageCode = botRole.getUser().getLanguageCode();
        if (messages.size() > 1 && validatorUtil.checkLanguageCode(botRole, messages.getLast())) {
            languageCode = messages.getLast().getText();
            messages.removeLast();
        }
        
        botService.addCreatorInfo(botRole, languageCode, messages);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.BOT_CREATOR_INFO_CREATED, botRole));
        LOGGER.debug("Message sent.");
    }

    public void addStart(BotRole botRole, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        validatorUtil.checkAtLeastExpectedMessages(botRole, messages, 1);
        String languageCode = botRole.getUser().getLanguageCode();
        if (messages.size() > 1 && validatorUtil.checkLanguageCode(botRole, messages.getLast())) {
            languageCode = messages.getLast().getText();
            messages.removeLast();
        }
        
        botService.addStart(botRole, languageCode, messages);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.BOT_START_CREATED, botRole));
        LOGGER.debug("Message sent.");
    }

    public void addTerms(BotRole botRole, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        validatorUtil.checkAtLeastExpectedMessages(botRole, messages, 1);
        String languageCode = botRole.getUser().getLanguageCode();
        if (messages.size() > 1 && validatorUtil.checkLanguageCode(botRole, messages.getLast())) {
            languageCode = messages.getLast().getText();
            messages.removeLast();
        }
        
        botService.addTerms(botRole, languageCode, messages);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.BOT_TERMS_CREATED, botRole));
        LOGGER.debug("Message sent.");
    }
}
