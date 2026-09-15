package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.config.properties.WebhookProperties;
import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.List;

import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class RegularClient extends CustomTelegramClient {
    private static final String URL = "/webhook/callback/%d";

    private final CommandHandlerManager commandHandlerManager;

    private final EntityUtil entityUtil;

    public RegularClient(Long botId, String botToken, LocalizationLoader loader, CertificateDao dao,
            CommandHandlerManager commandHandlerManager, EntityUtil entityUtil, WebhookProperties webhookProperties) {
        super(botId, botToken, loader, dao, webhookProperties);
        this.commandHandlerManager = commandHandlerManager;
        this.entityUtil = entityUtil;

        initialize();
    }

    public void reloadMenus() {
        logger.debug("Initializing command menus for bot " + botId + "...");
        setUpMenuButton();

        localizationLoader.getAvailableLanguageCodes().forEach(c -> setUpUserMenu(c));
        final UserEntity director = entityUtil.getDirector();

        setUpMenuForUserForRole(entityUtil.getDirectorBotRole(botId));
        final BotRole creatorRole = entityUtil.getCreator(botId);

        if (!creatorRole.getUser().getId().equals(director.getId())) {
            setUpMenuForUserForRole(creatorRole);
        }
    
        entityUtil.getSupport(botId).forEach(s -> setUpMenuForUserForRole(s));
        entityUtil.getMentors(botId).forEach(m -> setUpMenuForUserForRole(m));
        
        logger.debug("Command menus have been initialized for bot " + botId + ".");
    }

    public void setUpMenuForUserForRole(BotRole botRole) {
        final List<String> languageCodes = localizationLoader.getAvailableLanguageCodes();

        for (String code : languageCodes) {
            final SetMyCommands setMyCommands = SetMyCommands.builder()
                    .commands(parseToBotCommands(commandHandlerManager.getCommandsForRole(botRole.getRole()).stream()
                        .filter(c -> !BOT_LORD_COMMANDS.contains(c))
                        .toList(), code, botRole.getBot().languagesToList()))
                    .languageCode(code)
                    .scope(BotCommandScopeChat.builder().chatId(botRole.getUser().getId()).build())
                    .build();
            try {
                execute(setMyCommands);
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to set up " + botId
                        + " bot's command menu for role " + botRole.getRole().getType() + " for user "
                        + botRole.getUser().getId(), null, e);
            }
        }
    }

    public void removeMenuForUser(UserEntity user) {
        final List<String> languageCodes = localizationLoader.getAvailableLanguageCodes();

        for (String code : languageCodes) {
            final DeleteMyCommands deleteMyCommands = DeleteMyCommands.builder()
                    .scope(BotCommandScopeChat.builder().chatId(user.getId()).build())
                    .languageCode(code)
                    .build();
            try {
                execute(deleteMyCommands);
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to remove " + botId + " bot's command menu for user "
                        + user.getId(), null, e);
            }
        }
    }

    public void setUpUserMenu(String languageCode) {
        final SetMyCommands setMyCommands = SetMyCommands.builder()
                .commands(parseToBotCommands(commandHandlerManager.getCommandsForRole(
                    entityUtil.getRole(RoleType.USER)).stream()
                        .filter(c -> !BOT_LORD_COMMANDS.contains(c))
                        .toList(), languageCode, entityUtil.getBot(botId).languagesToList()))
                .scope(BotCommandScopeDefault.builder().build())
                .languageCode(languageCode)
                .build();
        try {
            execute(setMyCommands);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up " + botId + " bot's default menu", null, e);
        }
    }

    protected void initialize() {
        super.initialize(URL.formatted(botId));
        
        reloadMenus();
    }
}
