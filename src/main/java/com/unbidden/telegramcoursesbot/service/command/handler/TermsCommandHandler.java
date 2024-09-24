package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.util.Blockable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class TermsCommandHandler implements CommandHandler {
    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    @Blockable
    public void handle(Message message, String[] commandParts) {
        final Localization localization = localizationLoader.getLocalizationForUser(
            "service_terms", message.getFrom());
        bot.sendMessage(SendMessage.builder()
                .chatId(message.getFrom().getId())
                .text(localization.getData())
                .entities(localization.getEntities())
                .build());
    }

    @Override
    public String getCommand() {
        return "/terms";
    }
}
