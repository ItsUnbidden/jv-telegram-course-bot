package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.repository.BotRepository;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BotService {
    private static final Logger LOGGER = LogManager.getLogger(BotService.class);
    
    private final BotRepository botRepository;

    private final BotRoleRepository botRoleRepository;

    private final EntityUtil entityUtil;

    @Value("${telegram.bot.authorization.start_bot.token}")
    private String initialBotToken;

    @Value("${telegram.bot.authorization.bot_lord.token}")
    private String botLordToken;

    @Transactional(readOnly = true)
    public List<Bot> getRegularBots() {
        return botRepository.findAllRegularBots();
    }

    @Transactional(readOnly = true)
    public List<BotRole> getAllCreatorRoles() {
        return botRoleRepository.findAllCreatorRoles();
    }

    @Transactional
    public Bot createBot(UserEntity director, UserEntity creator, String token) {
        LOGGER.info("Creating a new bot...");
        final Bot bot = new Bot();

        bot.setToken(token);

        final List<BotRole> botRoles = new ArrayList<>();

        if (director.getId().equals(creator.getId())) {
            LOGGER.warn("Bot Creator is Director. No Creator role will be added.");
            botRoles.add(new BotRole(bot, director, entityUtil.getRole(RoleType.DIRECTOR), true));
        } else {
            botRoles.add(new BotRole(bot, creator, entityUtil.getRole(RoleType.CREATOR), true));
            botRoles.add(new BotRole(bot, director, entityUtil.getRole(RoleType.DIRECTOR), false));
        }
        
        botRepository.save(bot);
        botRoleRepository.saveAll(botRoles);

        return bot;
    }

    @Transactional
    public Bot updateInitialBot(UserEntity director) {
        LOGGER.info("Updating start bot token...");

        final Bot startBot = entityUtil.getStartBot();

        startBot.setToken(initialBotToken);

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
        LOGGER.info("Updating bot lord token...");

        final Bot botLord = entityUtil.getBotLord();

        botLord.setToken(botLordToken);

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
}
