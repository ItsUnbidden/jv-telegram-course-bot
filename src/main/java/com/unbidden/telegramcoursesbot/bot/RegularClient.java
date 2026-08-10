package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class RegularClient extends CustomTelegramClient {
    private static final String URL = "/webhook/callback/%d";

    private final CommandHandlerManager commandHandlerManager;

    private final EntityUtil entityUtil;

    private final Integer maxConnections;

    public RegularClient(@NonNull Bot bot, @NonNull UserService userService,
            @NonNull LocalizationLoader loader, @NonNull CertificateDao dao,
            @NonNull CommandHandlerManager commandHandlerManager,
            @NonNull EntityUtil entityUtil,
            @NonNull String baseUrl, @NonNull String secretToken,
            @Nullable String ip, int maxConnections, boolean isCustomCertificateIncluded) {
        super(bot, userService, loader, dao, baseUrl, secretToken,
                ip, isCustomCertificateIncluded);
        this.commandHandlerManager = commandHandlerManager;
        this.entityUtil = entityUtil;
        this.maxConnections = maxConnections;

        initialize();
    }

    public void reloadMenus() {
        logger.debug("Initializing command menus for bot " + bot.getId() + "...");
        setUpMenuButton();

        localizationLoader.getAvailableLanguageCodes().forEach(c -> setUpUserMenu(c));
        final UserEntity director = entityUtil.getDiretor();

        setUpMenuForUserForRole(director, entityUtil.getRole(RoleType.DIRECTOR));
        final UserEntity creator = entityUtil.getCreator(bot);

        if (!creator.getId().equals(director.getId())) {
            setUpMenuForUserForRole(entityUtil.getCreator(bot),
                    entityUtil.getRole(RoleType.CREATOR));
        }
    
        final Role supportRole = entityUtil.getRole(RoleType.SUPPORT);
        entityUtil.getSupport(bot).forEach(s -> setUpMenuForUserForRole(s, supportRole));

        final Role mentorRole = entityUtil.getRole(RoleType.MENTOR);
        entityUtil.getMentors(bot).forEach(m -> setUpMenuForUserForRole(m, mentorRole));
        
        logger.debug("Command menus have been initialized for bot " + bot.getId() + ".");
    }

    public void setUpMenuForUserForRole(@NonNull UserEntity user, @NonNull Role role) {
        final List<String> languageCodes = localizationLoader.getAvailableLanguageCodes();

        for (String code : languageCodes) {
            final SetMyCommands setMyCommands = SetMyCommands.builder()
                    .commands(parseToBotCommands(commandHandlerManager.getCommandsForRole(role).stream()
                        .filter(c -> !BOT_LORD_COMMANDS.contains(c))
                        .toList(), code))
                    .languageCode(code)
                    .scope(BotCommandScopeChat.builder().chatId(user.getId()).build())
                    .build();
            try {
                execute(setMyCommands);
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to set up " + bot.getId()
                        + " bot's command menu for role " + role.getType() + " for user "
                        + user.getId(), null, e);
            }
        }
    }

    public void removeMenuForUser(@NonNull UserEntity user) {
        final List<String> languageCodes = localizationLoader.getAvailableLanguageCodes();

        for (String code : languageCodes) {
            final DeleteMyCommands deleteMyCommands = DeleteMyCommands.builder()
                    .scope(BotCommandScopeChat.builder().chatId(user.getId()).build())
                    .languageCode(code)
                    .build();
            try {
                execute(deleteMyCommands);
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to remove " + bot.getId()
                        + " bot's command menu for user "
                        + user.getId(), null, e);
            }
        }
    }

    public void setUpUserMenu(@NonNull String languageCode) {
        final SetMyCommands setMyCommands = SetMyCommands.builder()
                .commands(parseToBotCommands(commandHandlerManager.getCommandsForRole(
                    entityUtil.getRole(RoleType.USER)).stream()
                        .filter(c -> !BOT_LORD_COMMANDS.contains(c))
                        .toList(), languageCode))
                .scope(BotCommandScopeDefault.builder().build())
                .languageCode(languageCode)
                .build();
        try {
            execute(setMyCommands);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up " + bot.getId()
                    + " bot's default menu", null, e);
        }
    }

    protected void initialize() {
        super.initialize(URL.formatted(bot.getId()), maxConnections);
        
        reloadMenus();
    }
}
