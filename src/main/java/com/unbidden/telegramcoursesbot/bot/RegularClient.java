package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.description.SetMyDescription;
import org.telegram.telegrambots.meta.api.methods.description.SetMyShortDescription;
import org.telegram.telegrambots.meta.api.methods.name.SetMyName;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class RegularClient extends CustomTelegramClient {
    private static final List<String> COMMAND_MENU_EXCEPTIONS = new ArrayList<>();
    
    private static final String MENU_COMMAND_DESCRIPTION = "menu_command_%s_description";

    private static final String SERVICE_BOT_NAME = "service_bot_%s_name";
    private static final String SERVICE_BOT_SHORT_DESCRIPTION = "service_bot_%s_short_description";
    private static final String SERVICE_BOT_FULL_DESCRIPTION = "service_bot_%s_full_description";

    private static final String URL = "/webhook/callback/%s";

    private final CommandHandlerManager commandHandlerManager;

    private final Integer maxConnections;

    public RegularClient(@NonNull Bot bot, @NonNull UserService userService,
            @NonNull LocalizationLoader loader, @NonNull CertificateDao dao,
            @NonNull CommandHandlerManager commandHandlerManager,
            @NonNull String baseUrl, @NonNull String secretToken,
            @Nullable String ip, int maxConnections, boolean isCustomCertificateIncluded) {
        super(bot, userService, loader, dao, baseUrl, secretToken,
                ip, isCustomCertificateIncluded);
        this.commandHandlerManager = commandHandlerManager;
        this.maxConnections = maxConnections;
        COMMAND_MENU_EXCEPTIONS.add("/maintenance");
        COMMAND_MENU_EXCEPTIONS.add("/refresh");
        COMMAND_MENU_EXCEPTIONS.add("/generalban");
        COMMAND_MENU_EXCEPTIONS.add("/botsettings");
        initialize();
    }

    public void reloadMenus() {
        logger.debug("Initializing command menus for bot " + bot.getName() + "...");
        setUpMenuButton();

        localizationLoader.getAvailableLanguageCodes().forEach(c -> setUpUserMenu(c));
        final UserEntity director = userService.getDiretor();

        setUpMenuForUserForRole(director, userService.getRole(RoleType.DIRECTOR));
        final UserEntity creator = userService.getCreator(bot);

        if (!creator.getId().equals(director.getId())) {
            setUpMenuForUserForRole(userService.getCreator(bot),
                    userService.getRole(RoleType.CREATOR));
        }
    
        final Role supportRole = userService.getRole(RoleType.SUPPORT);
        userService.getSupport(bot).forEach(s -> setUpMenuForUserForRole(s, supportRole));

        final Role mentorRole = userService.getRole(RoleType.MENTOR);
        userService.getMentors(bot).forEach(m -> setUpMenuForUserForRole(m, mentorRole));
        
        logger.debug("Command menus have been initialized for bot " + bot.getName() + ".");
    }

    public void setUpDescriptions() {
        for (String code : localizationLoader.getAvailableLanguageCodes()) {
            final SetMyDescription setMyDescription = SetMyDescription.builder()
                    .languageCode(code)
                    .description(localizationLoader.loadLocalization(
                        SERVICE_BOT_FULL_DESCRIPTION.formatted(bot.getName()), code).getData())
                    .build();
            try {
                execute(setMyDescription);
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to set up " + bot.getName()
                        + " bot's full description", null, e);
            }
        }
    }

    public void setUpShortDescriptions() {
        for (String code : localizationLoader.getAvailableLanguageCodes()) {
            final SetMyShortDescription setMyShortDescription = SetMyShortDescription.builder()
                    .languageCode(code)
                    .shortDescription(localizationLoader.loadLocalization(
                        SERVICE_BOT_SHORT_DESCRIPTION.formatted(bot.getName()), code).getData())
                    .build();
            try {
                execute(setMyShortDescription);
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to set up " + bot.getName()
                        + " bot's short description", null, e);
            }
        }
    }

    public void setUpNames() {
        for (String code : localizationLoader.getAvailableLanguageCodes()) {
            final SetMyName setMyName = SetMyName.builder()
                    .languageCode(code)
                    .name(localizationLoader.loadLocalization(
                        SERVICE_BOT_NAME.formatted(bot.getName()), code).getData())
                    .build();
            try {
                execute(setMyName);
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to set up " + bot.getName()
                        + " bot's name", null, e);
            }
        }
    }

    public void setUpMenuForUserForRole(@NonNull UserEntity user, @NonNull Role role) {
        final List<String> languageCodes = localizationLoader.getAvailableLanguageCodes();

        for (String code : languageCodes) {
            final SetMyCommands setMyCommands = SetMyCommands.builder()
                    .commands(parseToBotCommands(commandHandlerManager.getCommandsForRole(role),
                        code))
                    .languageCode(code)
                    .scope(BotCommandScopeChat.builder().chatId(user.getId()).build())
                    .build();
            try {
                execute(setMyCommands);
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to set up " + bot.getName()
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
                throw new TelegramException("Unable to remove " + bot.getName()
                        + " bot's command menu for user "
                        + user.getId(), null, e);
            }
        }
    }

    public void setUpUserMenu(@NonNull String languageCode) {
        final SetMyCommands setMyCommands = SetMyCommands.builder()
                .commands(parseToBotCommands(commandHandlerManager.getCommandsForRole(
                    userService.getRole(RoleType.USER)), languageCode))
                .scope(BotCommandScopeDefault.builder().build())
                .languageCode(languageCode)
                .build();
        try {
            execute(setMyCommands);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up " + bot.getName()
                    + " bot's default menu", null, e);
        }
    }

    private List<BotCommand> parseToBotCommands(List<String> commands, String languageCode) {
        return commands.stream()
                .filter(c -> !COMMAND_MENU_EXCEPTIONS.contains(c))
                .map(c -> (BotCommand)BotCommand.builder()
                    .command(c)
                    .description(localizationLoader.loadLocalization(
                        MENU_COMMAND_DESCRIPTION.formatted(c.replace("/", "")),
                        languageCode).getData())
                    .build())
                .toList();
    }

    protected void initialize() {
        super.initialize(URL.formatted(bot.getName()), maxConnections);
        
        reloadMenus();
    }
}
