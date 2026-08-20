package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BotLordClient extends CustomTelegramClient {
    private static final String URL = "/webhook/botlord";

    private static final int BOT_LORD_MAX_CONNECTIONS = 5;
    private static final String COMMANDS_LANGUAGE_CODE = "en";

    public BotLordClient(@NonNull String token, @NonNull String baseUrl,
            @Nullable String ip, @NonNull String secretToken, @NonNull Bot bot,
            @NonNull CertificateDao certificateDao, @NonNull LocalizationLoader localizationLoader,
            boolean isCustomCertificateIncluded) {
        super(bot, localizationLoader, certificateDao,
                baseUrl, secretToken, ip, isCustomCertificateIncluded);
        initialize();
    }

    protected void initialize() {
        super.initialize(URL, BOT_LORD_MAX_CONNECTIONS);
        setUpMenu();
    }

    public void setUpMenu() {
        final SetMyCommands setMyCommands = SetMyCommands.builder()
                .commands(parseToBotCommands(BOT_LORD_COMMANDS, COMMANDS_LANGUAGE_CODE))
                .scope(BotCommandScopeDefault.builder().build())
                .languageCode(COMMANDS_LANGUAGE_CODE)
                .build();
        try {
            execute(setMyCommands);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up bot lord's menu", null, e);
        }
    }
}
