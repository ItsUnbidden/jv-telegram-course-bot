package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class RefreshCommandHandler implements CommandHandler {
    private static final String COMMAND = "/refresh";

    private static final String SERVICE_REFRESH_SUCCESS = "service_refresh_success";
    
    private final UserService userService;

    private final LocalizationLoader localizationLoader;
    
    private final CustomTelegramClient client;

    @Autowired
    @Lazy
    private CommandHandlerManager commandHandlerManager;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final UserEntity user = userService.getUser(message.getFrom().getId());

        if (userService.isAdmin(user)) {
            client.setOnMaintenance(true);

            localizationLoader.reloadResourses();
            client.setUpMenuButton();
            localizationLoader.getAvailableLanguageCodes().forEach(c -> client.setUpUserMenu(c,
                    commandHandlerManager.getUserCommands()));
            userService.getAdminList().forEach(a -> client.setUpMenuForAdmin(a,
                    commandHandlerManager.getAllCommands()));
            
            client.setOnMaintenance(false);

            final Localization localization = localizationLoader.getLocalizationForUser(
                    SERVICE_REFRESH_SUCCESS, message.getFrom());
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
