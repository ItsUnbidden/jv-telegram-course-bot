package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class LocalizationCommandHandler implements CommandHandler {
    private final TelegramBot bot;

    private final LocalizationLoader localizationLoader;

    @Override
    public void handle(Message message, String[] commandParts) {
        if (commandParts.length > 2 && bot.isAdmin(new UserEntity(message.getFrom()))) {
            Localization localization = localizationLoader.loadLocalization(commandParts[1],
                    commandParts[2]);
            bot.sendMessage(SendMessage.builder()
                    .chatId(message.getFrom().getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
        }
    }

    @Override
    public String getCommand() {
        return "/localization";
    }
}
