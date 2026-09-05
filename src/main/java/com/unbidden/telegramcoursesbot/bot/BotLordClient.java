package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.config.properties.WebhookProperties;
import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.util.TextUtil;

import java.util.List;

import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BotLordClient extends CustomTelegramClient {
    private static final String URL = "/webhook/botlord";

    private final TextUtil textUtil;

    public BotLordClient(Long botId, String token, LocalizationLoader localizationLoader,
            CertificateDao certificateDao, WebhookProperties webhookProperties, TextUtil textUtil) {
        super(botId, token, localizationLoader, certificateDao, webhookProperties);
        this.textUtil = textUtil;
        initialize();
    }

    protected void initialize() {
        super.initialize(URL);
        setUpMenu();
    }

    public void setUpMenu() {
        final String highestLanguageCode = textUtil.getLanguagePriority().getFirst();
        final SetMyCommands setMyCommands = SetMyCommands.builder()
                .commands(parseToBotCommands(BOT_LORD_COMMANDS, highestLanguageCode, List.of()))
                .scope(BotCommandScopeDefault.builder().build())
                .languageCode(highestLanguageCode)
                .build();
        try {
            execute(setMyCommands);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up bot lord's menu", null, e);
        }
    }
}
