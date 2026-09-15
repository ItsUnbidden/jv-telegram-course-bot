package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.config.properties.BaseProperties;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.repository.BotRepository;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class BotService {
    private static final Logger LOGGER = LogManager.getLogger(BotService.class);
    
    private final BotRepository botRepository;

    private final BotRoleRepository botRoleRepository;

    private final ContentMappingRepository contentMappingRepository;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader loader;

    private final EntityUtil entityUtil;

    private final BaseProperties baseProperties;

    @Transactional(readOnly = true)
    public List<Bot> getRegularBots() {
        return botRepository.findAllRegularBots();
    }

    @Transactional(readOnly = true)
    public List<BotRole> getAllCreatorRoles() {
        return botRoleRepository.findAllCreatorRoles();
    }

    @Transactional
    public Bot createBot(BotRole botRole, Long creatorId, String token) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(creatorId, "creatorId cannot be null");
        Assert.notNull(token, "token cannot be null");

        LOGGER.info("Creating a new bot...");
        final Bot bot = new Bot();

        bot.setToken(token);

        final UserEntity dirRef = entityUtil.getUserReference(botRole.getUser().getId());
        final List<BotRole> botRoles = new ArrayList<>();

        if (botRole.getUser().getId().equals(creatorId)) {
            LOGGER.warn("Bot Creator is Director. No Creator role will be added.");
            botRoles.add(new BotRole(bot, dirRef, entityUtil.getRole(RoleType.DIRECTOR), true));
        } else {
            botRoles.add(new BotRole(bot, entityUtil.getUser(botRole, creatorId), entityUtil.getRole(RoleType.CREATOR), true));
            botRoles.add(new BotRole(bot, dirRef, entityUtil.getRole(RoleType.DIRECTOR), false));
        }
        
        botRepository.save(bot);
        botRoleRepository.saveAll(botRoles);

        return bot;
    }

    @Transactional
    public Bot updateInitialBot(UserEntity director) {
        Assert.notNull(director, "director cannot be null");

        LOGGER.info("Updating start bot token...");

        final Bot startBot = entityUtil.getStartBot();

        startBot.setToken(baseProperties.startBotToken());

        if (!botRoleRepository.existsByBotIdAndUserId(startBot.getId(), director.getId())) {
            LOGGER.debug("Director does not have a bot role in the start bot.");
            final List<BotRole> roles = botRoleRepository.findByBotId(startBot.getId());
                
            if (roles.isEmpty()) {
                LOGGER.debug("Start bot does not have any roles. Creating a new Director role...");
            } else {
                LOGGER.debug("Start bot has some old roles. Deleting them and creating "
                        + "a new one for the current Director " + director.getId() + "...");
                botRoleRepository.deleteAllInBatch(roles);

            }
            botRoleRepository.save(new BotRole(startBot, director,
                        entityUtil.getRole(RoleType.DIRECTOR), true));
        }
        LOGGER.info("Start bot has been updated.");

        return startBot;
    }

    @Transactional
    public Bot updateBotLord(UserEntity director) {
        Assert.notNull(director, "director cannot be null");

        LOGGER.info("Updating bot lord token...");

        final Bot botLord = entityUtil.getBotLord();

        botLord.setToken(baseProperties.botLordToken());

        if (!botRoleRepository.existsByBotIdAndUserId(botLord.getId(), director.getId())) {
            LOGGER.debug("Director does not have a bot role in bot lord.");
            final List<BotRole> roles = botRoleRepository.findByBotId(botLord.getId());
                
            if (roles.isEmpty()) {
                LOGGER.debug("Bot lord does not have any roles. Creating a new Director role...");
            } else {
                LOGGER.debug("Bot lord has some old roles. Deleting them and creating "
                        + "a new one for the current director " + director.getId() + "...");
                botRoleRepository.deleteAllInBatch(roles);

            }
            botRoleRepository.save(new BotRole(botLord, director,
                        entityUtil.getRole(RoleType.DIRECTOR), true));
        }
        LOGGER.info("Bot lord has been updated.");

        return botLord;
    }

    @Transactional 
    public Bot addCreatorInfo(BotRole botRole, String languageCode, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        final Bot bot = entityUtil.getBot(botRole.getBot().getId());

        if (bot.getCreatorInfo() != null) {
            throw new ForbiddenOperationException("Bot " + bot.getId() + " already has creator info " + bot.getCreatorInfo().getId() + ".",
                    loader.localize(Localizations.Error.BOT_CREATOR_INFO_PRESENT, botRole));
        }
        LOGGER.info("User " + botRole.getUser().getId() + " is adding a creator info mapping to bot " + bot.getId() + "...");

        final ContentMapping mapping = new ContentMapping();

        mapping.setPosition(0);
        mapping.setContent(List.of(contentService.parseAndPersistContent(botRole, messages, languageCode)));

        bot.setCreatorInfo(contentMappingRepository.save(mapping));

        return bot;
    }

    @Transactional 
    public Bot addStart(BotRole botRole, String languageCode, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        final Bot bot = entityUtil.getBot(botRole.getBot().getId());

        if (bot.getStart() != null) {
            throw new ForbiddenOperationException("Bot " + bot.getId() + " already has start mapping " + bot.getStart().getId() + ".",
                    loader.localize(Localizations.Error.BOT_START_PRESENT, botRole));
        }
        LOGGER.info("User " + botRole.getUser().getId() + " is adding a start mapping to bot " + bot.getId() + "...");

        final ContentMapping mapping = new ContentMapping();

        mapping.setPosition(0);
        mapping.setContent(List.of(contentService.parseAndPersistContent(botRole, messages, languageCode)));

        bot.setStart(contentMappingRepository.save(mapping));

        return bot;
    }

    @Transactional 
    public Bot addTerms(BotRole botRole, String languageCode, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        final Bot bot = entityUtil.getBot(botRole.getBot().getId());

        if (bot.getTerms() != null) {
            throw new ForbiddenOperationException("Bot " + bot.getId() + " already has terms " + bot.getTerms().getId() + ".",
                    loader.localize(Localizations.Error.BOT_TERMS_PRESENT, botRole));
        }
        LOGGER.info("User " + botRole.getUser().getId() + " is adding a terms mapping to bot " + bot.getId() + "...");

        final ContentMapping mapping = new ContentMapping();

        mapping.setPosition(0);
        mapping.setContent(List.of(contentService.parseAndPersistContent(botRole, messages, languageCode)));

        bot.setTerms(contentMappingRepository.save(mapping));

        return bot;
    }
}
