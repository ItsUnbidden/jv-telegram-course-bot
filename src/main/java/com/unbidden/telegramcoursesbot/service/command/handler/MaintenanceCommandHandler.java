package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
            
            bot.sendMessage(localizationLoader.getSendMessage(
                    "message_on_maintenance_status_change", message.getFrom(), paramsMap));
        }
    }

    @Override
    public String getCommand() {
        return "/maintenance";
    }
}
