package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class MaintenanceCommandHandler implements CommandHandler {
    private final TelegramBot bot;

    private final LocalizationLoader localizationLoader;

    @Override
    public void handle(Message message, String[] commandParts) {
        if (bot.isAdmin(new UserEntity(message.getFrom()))) {
            bot.setOnMaintenance(!bot.isOnMaintenance());
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("${status}", !bot.isOnMaintenance());
            
            final Localization localization = localizationLoader.getLocalizationForUser(
                "service_on_maintenance_status_change", message.getFrom(), paramsMap);
            bot.sendMessage(SendMessage.builder()
                    .chatId(message.getFrom().getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
        }
    }

    @Override
    public String getCommand() {
        return "/maintenance";
    }
}
