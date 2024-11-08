package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class MaintenanceCommandHandler implements CommandHandler {
    private static final String COMMAND = "/maintenance";
    
    private static final String PARAM_STATUS = "${status}";
    
    private static final String SERVICE_ON_MAINTENANCE_STATUS_CHANGE =
            "service_on_maintenance_status_change";

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final UserEntity user = userService.getUser(message.getFrom().getId());

        if (userService.isAdmin(user)) {
            client.setOnMaintenance(!client.isOnMaintenance());
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put(PARAM_STATUS, !client.isOnMaintenance());
            
            final Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_ON_MAINTENANCE_STATUS_CHANGE, message.getFrom(), paramsMap);
            client.sendMessage(user, localization);
        }
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }
}
