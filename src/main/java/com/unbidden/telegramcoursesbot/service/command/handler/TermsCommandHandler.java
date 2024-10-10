package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.util.Blockable;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class TermsCommandHandler implements CommandHandler {
    private static final String COMMAND = "/terms";

    private static final String SERVICE_TERMS = "service_terms";

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    @Blockable
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final Localization localization = localizationLoader.getLocalizationForUser(
            SERVICE_TERMS, message.getFrom());
        bot.sendMessage(SendMessage.builder()
                .chatId(message.getFrom().getId())
                .text(localization.getData())
                .entities(localization.getEntities())
                .build());
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }
}
