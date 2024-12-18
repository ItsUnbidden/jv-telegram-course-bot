package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BotFatherClient extends CustomTelegramClient {
    private static final String MENU_COMMAND_DESCRIPTION = "menu_command_%s_description";
    
    private static final String URL = "/webhook/botfather";
    private static final int BOT_FATHER_MAX_CONNECTIONS = 5;
    private static final String COMMANDS_LANGUAGE_CODE = "en";

    private static final List<String> COMMANDS = new ArrayList<>();

    public BotFatherClient(@NonNull String token, @NonNull String baseUrl,
            @Nullable String ip, @NonNull String secretToken, @NonNull Bot bot,
            @NonNull CertificateDao certificateDao, @NonNull UserService userService,
            @NonNull LocalizationLoader localizationLoader, boolean isCustomCertificateIncluded) {
        super(bot, userService, localizationLoader, certificateDao,
                baseUrl, secretToken, ip, isCustomCertificateIncluded);
        COMMANDS.add("/maintenance");
        COMMANDS.add("/refresh");
        COMMANDS.add("/generalban");
        COMMANDS.add("/botsettings");
        COMMANDS.add("/generalpost");
        initialize();
    }

    protected void initialize() {
        super.initialize(URL, BOT_FATHER_MAX_CONNECTIONS);
        setUpMenu();
    }

    public void setUpMenu() {
        final SetMyCommands setMyCommands = SetMyCommands.builder()
                .commands(parseToBotCommands(COMMANDS, COMMANDS_LANGUAGE_CODE))
                .scope(BotCommandScopeDefault.builder().build())
                .languageCode(COMMANDS_LANGUAGE_CODE)
                .build();
        try {
            execute(setMyCommands);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up botfather's menu", null, e);
        }
    }

    private List<BotCommand> parseToBotCommands(List<String> commands, String languageCode) {
        return commands.stream()
                .map(c -> (BotCommand)BotCommand.builder()
                    .command(c)
                    .description(localizationLoader.loadLocalization(
                        MENU_COMMAND_DESCRIPTION.formatted(c.replace("/", "")),
                        languageCode).getData())
                    .build())
                .toList();
    }
}
